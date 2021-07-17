package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

import java.io.Reader;
import java.io.LineNumberReader;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import java.util.regex.Pattern;

import com.ibm.nmon.data.*;
import com.ibm.nmon.data.Process;
import com.ibm.nmon.data.transform.*;
import com.ibm.nmon.util.DataHelper;

/**
 * A parser for NMON files. The result of a successfully parsed file will be a populated {@link NMONDataSet} object.
 */
public final class NMONParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NMONParser.class);

    private static final SimpleDateFormat NMON_FORMAT = new SimpleDateFormat("HH:mm:ss dd-MMM-yyyy",
            java.util.Locale.US);
    private static final Pattern DATA_SPLITTER = Pattern.compile(",");

    private LineNumberReader in = null;
    private DataRecord currentRecord = null;

    private NMONDataSet data = null;

    private String[] topFields = null;
    private int topCommandIndex = -1;

    private String[] summaryFields = null;

    private int fileCPUs = 1;
    private boolean seenFirstDataType = false;
    private boolean isAIX = false;
    private boolean scaleProcessesByCPU = true;

    private final Map<Integer, Process> processes = new java.util.HashMap<Integer, Process>();
    private final Map<String, StringBuilder> systemInfo = new java.util.HashMap<String, StringBuilder>();

    private final List<DataTransform> transforms = new java.util.ArrayList<DataTransform>();
    private final List<DataPostProcessor> processors = new java.util.ArrayList<DataPostProcessor>();

    public NMONParser() {
        processors.add(new NetworkTotalPostProcessor("NET"));
        processors.add(new NetworkTotalPostProcessor("SEA"));
        processors.add(new EthernetTotalPostProcessor("NET"));
        processors.add(new EthernetTotalPostProcessor("SEA"));
    }

    public NMONDataSet parse(File file, TimeZone timeZone, boolean scaleProcessesByCPU) throws IOException {
        return parse(file.getAbsolutePath(), timeZone, scaleProcessesByCPU);
    }

    public NMONDataSet parse(String filename, TimeZone timeZone, boolean scaleProcessesByCPU) throws IOException {
        return parse(filename, new java.io.FileReader(filename), timeZone, scaleProcessesByCPU);
    }

    public NMONDataSet parse(String datasetName, Reader reader, TimeZone timeZone, boolean scaleProcessesByCPU)
            throws IOException {
        long start = System.nanoTime();

        this.scaleProcessesByCPU = scaleProcessesByCPU;

        in = new LineNumberReader(reader);

        try {
            data = new NMONDataSet(datasetName);

            NMON_FORMAT.setTimeZone(timeZone);

            data.setMetadata("parsed_gmt_offset",
                    Double.toString(timeZone.getOffset(System.currentTimeMillis()) / 3600000.0d));

            String line = parseHeaders();

            // no timestamp records after the headers => no other data
            if ((line == null) || !line.startsWith("ZZZZ")) {
                throw new IOException("file '" + datasetName + "' does not appear to have any data records");
            }
            // else line contains the first timestamp record, so start parsing

            for (DataPostProcessor processor : processors) {
                processor.addDataTypes(data);
            }

            do {
                parseLine(line);
            } while ((line = in.readLine()) != null);

            // save file's system info
            for (String name : systemInfo.keySet()) {
                String value = systemInfo.get(name).toString();
                data.setSystemInfo(name, value);
            }

            // final record completes when the file is completely read
            if (currentRecord != null) {
                completeCurrentRecord();
            }

            DataHelper.aggregateProcessData(data, LOGGER);

            return data;
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (Exception e) {
                    // ignore
                }

                in = null;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                        (System.nanoTime() - start) / 1000000.0d);
            }

            data = null;
            currentRecord = null;
            topFields = null;
            topCommandIndex = -1;
            summaryFields = null;
            fileCPUs = 1;
            seenFirstDataType = false;
            isAIX = false;

            processes.clear();
            systemInfo.clear();
            transforms.clear();
        }
    }

    private String parseHeaders() throws IOException {
        String line = null;

        // continue reading the NMON file until the first timestamp (ZZZZ) record or the file ends
        while ((line = in.readLine()) != null) {
            if (line.startsWith("AAA")) {
                String[] values = DATA_SPLITTER.split(line);

                if (!values[1].startsWith("note") && (values.length > 2)) {
                    // Linux NMON OS string has extra kernel and architecture info
                    if ("OS".equals(values[1])) {
                        data.setMetadata("OS", DataHelper.newString(values[2] + ' ' + values[3]));
                        data.setMetadata("ARCH", DataHelper.newString(values[5]));
                    }
                    else if ("MachineType".equals(values[1])) {
                        data.setMetadata("MachineType", DataHelper.newString(values[2] + ' ' + values[3]));
                    }
                    else if ("LPARNumberName".equals(values[1])) {
                        // AAA,LPARNumberName,none => whole system LPAR
                        if (values.length > 3) {
                            data.setMetadata("LPARNumber", DataHelper.newString(values[2]));
                            data.setMetadata("LPARName", DataHelper.newString(values[3]));
                        }
                    }
                    else if ("cpus".equals(values[1])) {
                        // use the current CPU count, not the max if available
                        if (values.length == 4) {
                            data.setMetadata(DataHelper.newString(values[1]), DataHelper.newString(values[3]));
                        }
                        else {
                            data.setMetadata(DataHelper.newString(values[1]), DataHelper.newString(values[2]));
                        }
                    }
                    else {
                        data.setMetadata(DataHelper.newString(values[1]), DataHelper.newString(values[2]));
                    }
                }
            }
            else if (line.startsWith("BBBP")) {
                parseBBBP(DATA_SPLITTER.split(line));
            }
            else if (line.startsWith("TOP")) {
                String[] values = DATA_SPLITTER.split(line);

                // TOP data has a bogus extra header line of "TOP,%CPU Utilization"
                // look for 'TOP,+PID,Time,...' instead
                if ("+PID".equals(values[1])) {
                    parseTopFields(values);
                }
            }
            else if (line.startsWith("ZZZZ")) {
                // headers end when data starts
                break;
            }
            else if (line.startsWith("BBB")) {
                parseSystemInfo(DATA_SPLITTER.split(line));
            }
            else if (line.startsWith("UARG")) {
                // AIX puts UARG type definition in header - ignore
            }
            else if (line.startsWith("SUMMARY")) {
                parseSummaryFields(DATA_SPLITTER.split(line));
            }
            else if (line.isEmpty()) {
                continue;
            }
            else {
                // AAA (metadata) records should be completed before first data type definition
                // so, the transforms can be built now along with the actual number of CPUs
                if (!seenFirstDataType) {
                    transforms.add(new CPUBusyTransform());
                    transforms.add(new DiskTotalTransform());
                    // transforms.add(new NetworkTotalTransform());

                    if (data.getMetadata("AIX") != null) {
                        isAIX = true;

                        transforms.add(new AIXMemoryTransform());
                        transforms.add(new AIXLPARTransform());
                        transforms.add(new AIXCPUTransform());
                    }
                    else {
                        transforms.add(new LinuxNetPacketTransform());
                        transforms.add(new LinuxMemoryTransform());
                    }

                    String temp = data.getMetadata("cpus");

                    if (temp != null) {
                        try {
                            fileCPUs = Integer.parseInt(temp);
                        }
                        catch (NumberFormatException nfe) {
                            // ignore and leave set to 1
                        }
                    }

                    seenFirstDataType = true;
                }

                DataType type = buildDataType(DATA_SPLITTER.split(line));

                if (type != null) {
                    data.addType(type);
                }
            }
        }

        return line;
    }

    private static final java.util.Set<String> IGNORED_TYPES = java.util.Collections
            .unmodifiableSet(new java.util.HashSet<String>(
                    java.util.Arrays.asList("AVM-IN-MB", "NO-PBUF-COUNT", "NO-PSBUF-COUNT", "NO-JFS2-FSBUF-COUNT")));

    private void parseLine(String line) {
        if (line.startsWith("ZZZZ")) {
            // add the previous record on a new timestamp
            if (currentRecord != null) {
                completeCurrentRecord();
            }

            currentRecord = parseTimestamp(line);
        }
        else if (line.startsWith("ERROR")) {
            // TODO handle this?
            return;
        }
        else {
            String[] values = DATA_SPLITTER.split(line);

            if (currentRecord == null) {
                if (IGNORED_TYPES.contains(values[0])) {
                    return;
                }
                else {
                    throw new IllegalStateException("current record is null at line " + in.getLineNumber());
                }
            }

            if (values.length < 2) {
                LOGGER.warn("skipping invalid data record '{}' starting at line {}", line, in.getLineNumber());
                return;
            }

            String timestamp = null;
            boolean isTop = "TOP".equals(values[0]);
            boolean isUarg = "UARG".equals(values[0]);

            // get the timestamp reference TXXXX
            // TOP records have pid as the 2nd column, then the reference
            if (isTop) {
                timestamp = values[2];
            }
            else {
                timestamp = values[1];
            }

            if (timestamp.startsWith("T")) {
                DataType type = data.getType(values[0]);

                if (timestamp.equals(currentRecord.getTimestamp())) {
                    if (isUarg) {
                        parseUARG(values);
                    }
                    else if (isTop) {
                        // assume TOP data type is created in the header
                        parseTopData(values);
                    }
                    else if ("SUMMARY".equals(values[0])) {
                        if (summaryFields == null) {
                            LOGGER.warn("undefined data type {} at line {}", values[0], in.getLineNumber());
                            return;
                        }

                        int commandIdx = values.length - 1; // command name is the last value
                        type = data.getType(SubDataType.buildId("SUMMARY", values[commandIdx]));

                        if (type == null) {
                            type = new SubDataType("SUMMARY", values[commandIdx], "Summary of Processes", false,
                                    summaryFields);
                            data.addType(type);
                        }

                        // remove the trailing command
                        String[] withoutCommand = new String[values.length - 1];
                        System.arraycopy(values, 0, withoutCommand, 0, commandIdx);
                        parseData(type, withoutCommand);
                    }
                    else {
                        if (type == null) {
                            if ("VM".equals(values[0])) {
                                // fix for issue #7
                                // NMON outputs the VM data type at T0001
                                // older versions contain the timestamp
                                // newer versions are handled below
                                String[] newValues = new String[values.length - 1];
                                newValues[0] = values[0];
                                System.arraycopy(values, 2, newValues, 1, values.length - 2);

                                type = buildDataType(newValues);
                                data.addType(type);
                            }
                            else {
                                LOGGER.warn("undefined data type {} at line {}", values[0], in.getLineNumber());
                            }
                        }
                        else {
                            parseData(type, values);
                        }
                    }
                }
                else {
                    LOGGER.warn("misplaced record at line {}; expected timestamp {} but got {}",
                            new Object[] { in.getLineNumber(), currentRecord.getTimestamp(), timestamp });
                }
            }
            else {
                // current line does not have a TXXXX record
                // ignore TOP and UARG data types
                if (!isTop && !isUarg) {
                    // AIX puts BBBP at then end of the file too
                    if ("BBBP".equals(values[0])) {
                        parseBBBP(values);
                    }
                    else if ("AAA".equals(values[0])) {
                        // ignore AAA records not in the header
                    }
                    // handle case where other BBB records wrote later in the file
                    else if (values[0].startsWith("BBB")) {
                        parseSystemInfo(values);
                    }
                    else if ("SUMMARY".equals(values[0])) {
                        parseSummaryFields(DATA_SPLITTER.split(line));
                    }
                    // otherwise, assume it is a new data type since data types can be added at any
                    // time in the NMON file
                    else if (data.getType(values[0]) == null) {
                        DataType type = buildDataType(values);

                        if (type != null) {
                            if (type.getId().equals("NO-JFS2-FSBUF-COUNT")) {
                                // hack to handle AVM-IN-MB, etc when added at the end of the file
                                completeCurrentRecord();
                            }

                            if (!IGNORED_TYPES.contains(type.getId())) {
                                data.addType(type);
                            }
                        }
                    }
                }
            }
        }
    }

    private DataRecord parseTimestamp(String line) {
        String[] values = DATA_SPLITTER.split(line);
        long time = 0;

        if (values.length != 4) {
            LOGGER.warn("skipping invalid data record '{}' starting at line {}", line, in.getLineNumber());
            return null;
        }
        else {
            try {
                time = NMON_FORMAT.parse(values[2] + ' ' + values[3]).getTime();
                long previous = data.getEndTime();

                if (time < previous) {
                    String temp = data.getMetadata("interval");

                    if (temp == null) {
                        LOGGER.error(
                                "time {} is less than previous {} at line {}" + "; no interval defined in AAA records",
                                new Object[] { time, previous, in.getLineNumber() });
                        throw new IllegalArgumentException("time is less than previous in ZZZZ " + values[1]);
                    }
                    else {
                        int interval = Integer.parseInt(temp);
                        time = previous + (interval * 1000); // interval is in seconds

                        LOGGER.warn(
                                "time {} is less than previous {} at line {}"
                                        + ", guessing at next time by using an interval of {}s",
                                new Object[] { time, previous, in.getLineNumber(), interval });
                    }
                }

                DataRecord record = new DataRecord(time, DataHelper.newString(values[1]));
                return record;

            }
            catch (ParseException pe) {
                LOGGER.warn("could not parse time {}, {} at line {}",
                        new Object[] { values[2], values[3], in.getLineNumber() });
                return null;
            }
        }
    }

    private void parseBBBP(String[] values) {
        // ignore header lines that only have BBBP, line number, info id
        if (values.length == 4) {
            if (values[3].charAt(0) == '\t') {
                return;
            }

            String command = DataHelper.newString(values[2]);
            StringBuilder builder = systemInfo.get(command);

            if (builder == null) {
                builder = new StringBuilder(256);
                systemInfo.put(command, builder);
            }
            else {
                builder.append('\n');
            }

            if (values[3].charAt(0) == '"') {
                // remove leading and trailing "
                builder.append(values[3], 1, values[3].length() - 1);
            }
            else {
                builder.append(values[3]);
            }
        }
    }

    private void parseSystemInfo(String[] values) {
        StringBuilder builder = systemInfo.get(values[0]);

        if (builder == null) {
            builder = new StringBuilder(256);
            systemInfo.put(DataHelper.newString(values[0]), builder);
        }
        else {
            // i = 2 => skip line number
            for (int i = 2; i < values.length - 1; i++) {
                builder.append(values[i]);
                builder.append(',');
            }

            builder.append(values[values.length - 1]);
            builder.append('\n');
        }
    }

    private void parseData(DataType type, String[] values) {
        List<Integer> toSkip = TYPE_SKIP_INDEXES.get(type.getId());

        if (toSkip == null) {
            toSkip = java.util.Collections.emptyList();
        }

        // + 2 => skip data type & timestamp
        double[] recordData = new double[values.length - 2 - toSkip.size()];

        int i = 2;
        int n = 0;

        // note try is outside the for loop since we want to skip the entire data record if any part
        // of is it bad
        try {
            for (; i < values.length; i++) {
                if (toSkip.contains(i)) {
                    continue;
                }

                String data = values[i];
                // 'nan' only appears in file sizes for virtual files like
                // rpc_pipefs; assume this is equivalent to 0
                if ("".equals(data) || data.contains("nan")) {
                    recordData[n] = 0;
                }
                else if ("INF".equals(data)) {
                    recordData[n] = Double.POSITIVE_INFINITY;
                }
                else {
                    recordData[n] = Double.parseDouble(data);
                }

                ++n;
            }
        }
        catch (NumberFormatException nfe) {
            LOGGER.warn("{}: invalid numeric data '{}' at line {}, column {}",
                    new Object[] { currentRecord.getTimestamp(), values[i], in.getLineNumber(), (i + 1) });
        }

        for (DataTransform transform : transforms) {
            if (transform.isValidFor(type.getId(), null)) {
                try {
                    recordData = transform.transform(type, recordData);
                }
                catch (Exception e) {
                    LOGGER.warn(currentRecord.getTimestamp() + ": could not complete transform "
                            + transform.getClass().getSimpleName() + " at line " + in.getLineNumber(), e);
                }
                break;
            }
        }

        try {
            currentRecord.addData(type, recordData);
        }
        catch (IllegalArgumentException ile) {
            if (type.getFieldCount() == recordData.length) {
                throw ile;
            }

            // else assume wrong number of columns, so add missing data in at the end
            double[] newData = new double[type.getFieldCount()];
            System.arraycopy(recordData, 0, newData, 0, recordData.length);

            // assume double arrays default to 0, so no need to fill in the rest
            LOGGER.warn("{}: DataType {} defines {} fields but there are only {} values; missing values set to 0",
                    new Object[] { currentRecord.getTimestamp(), type.getId(), type.getFieldCount(),
                            recordData.length });

            recordData = newData;

            // allow this to fail if updated the columns did not solve the issue
            currentRecord.addData(type, recordData);
        }
    }

    private void parseTopFields(String[] values) {
        // assume TOP record is like TOP,pid,TXXX,...,command,...
        // so remove TOP, pid, TXXX, and command
        // last field in AIX is WLMclass; skip that too
        // add 1 for calculated Wait% utilization
        topFields = new String[values.length - (isAIX ? 5 : 4) + 1];

        // 3 => skip TOP, pid & timestamp
        int valuesIdx = 3;
        int fieldsIdx = 0;

        while (valuesIdx < values.length) {
            if ("Command".equals(values[valuesIdx])) {
                // command line is 2nd to last in AIX
                // in Linux there may be other data after the command
                topCommandIndex = valuesIdx++;
            }
            else if ("WLMclass".equals(values[valuesIdx])) {
                ++valuesIdx;
            }
            else {
                if (fieldsIdx == 3) {
                    topFields[fieldsIdx++] = "%Wait";
                }
                else {
                    topFields[fieldsIdx++] = DataHelper.newString(values[valuesIdx++]);
                }
            }
        }
    }

    private void parseSummaryFields(String[] values) {
        // 2 => skip data type, timestamp and command
        // command will be the subTypeId of the DataType
        summaryFields = new String[values.length - 3];

        for (int i = 0; i < summaryFields.length; i++) {
            summaryFields[i] = DataHelper.newString(values[i + 2]);
        }
    }

    private void parseTopData(String[] values) {
        // assume TOP record is like TOP,pid,TXXX,...,command
        // add 1 back in for generated Wait%
        double[] recordData = new double[topFields.length];

        int n = 1;

        int pid = -1;
        String name = values[topCommandIndex];

        // note try is outside the for loop since we want to skip the entire data record if any part
        // of is it bad
        try {
            pid = Integer.parseInt(values[n++]);

            // skip timestamp
            ++n;

            for (int i = 0; i < recordData.length; i++) {
                if (n == topCommandIndex) {
                    ++n;
                }

                if (i == 3) {
                    recordData[i] = recordData[0] - recordData[1] - recordData[2];

                    // Wait% is less than 0 assume rounding errors in CPU%
                    // fix errors and set Wait% to 0;
                    if (recordData[i] < 0) {
                        recordData[0] -= recordData[i];
                        recordData[i] = 0;
                    }
                }
                else {
                    recordData[i] = Double.parseDouble(values[n++]);
                }
            }
        }
        catch (NumberFormatException nfe) {
            LOGGER.warn("{}: invalid numeric data '{}' at line {}, column {}",
                    new Object[] { currentRecord.getTimestamp(), values[n], in.getLineNumber(), (n - 1) });
            return;
        }

        Process process = processes.get(pid);
        boolean newProcess = false;

        ProcessDataType processType = null;

        if (process != null) {
            // process could have the same name but still be different
            // assume UARG records appear after TOP and handle that case in parseUARG()
            if (process.getName().equals(name)) {
                processType = data.getType(process);
            }
            else {
                LOGGER.debug("process id {} reused; '{}' is now '{}'", new Object[] { pid, process.getName(), name });

                process.setEndTime(currentRecord.getTime());

                newProcess = true;
            }
        }
        else {
            newProcess = true;
        }

        if (newProcess) {
            process = new Process(pid, currentRecord.getTime(), DataHelper.newString(name));
            processes.put(pid, process); // overwrites old process

            processType = new ProcessDataType(process, topFields);
            data.addType(processType);
            data.addProcess(process);
        }

        process.setEndTime(currentRecord.getTime());

        if (scaleProcessesByCPU) {
            currentRecord.addData(processType, scaleProcessDataByCPUs(processType, recordData));
        }
        else {
            currentRecord.addData(processType, recordData);
        }
    }

    private void parseUARG(String[] values) {
        int cmdLineIdx = 4;

        if (isAIX) {
            cmdLineIdx = 8;
        }

        if (values.length < cmdLineIdx) {
            // AIX ocassionally puts out bogus UARG header lines, ignore
            return;
        }

        String commandLine = values[cmdLineIdx];

        // original command line may contain commas, rebuild it
        for (int i = cmdLineIdx + 1; i < values.length; i++) {
            commandLine += ',' + values[i];
        }

        commandLine = DataHelper.newString(commandLine);
        int pid = -1;

        try {
            pid = Integer.parseInt(values[2]);
        }
        catch (NumberFormatException nfe) {
            LOGGER.warn("invalid process id {} at line {}", values[2], in.getLineNumber());
            return;
        }

        Process process = processes.get(pid);

        if (process == null) {
            LOGGER.warn("misplaced UARG record at line {}, no process with pid {} not defined yet", in.getLineNumber(),
                    pid);
            return;
        }

        if ("".equals(process.getCommandLine())) {
            process.setCommandLine(commandLine);
        }
        else if (!process.getCommandLine().equals(commandLine)) {
            // process ids can be reused; getTopData() covers processes with different names
            // handle the case where the id is reused, process is the same name but the command line
            // is different
            // note that it's possible to have all 3 be the same for a _different_ process, but
            // there is no way to tell that it is actually different in that case, so do not
            // bother trying to handle
            LOGGER.debug("process id {} reused; command line for '{}' is now '{}'",
                    new Object[] { pid, process.getName(), commandLine });

            process.setEndTime(currentRecord.getTime());
            ProcessDataType oldProcessType = data.getType(process);

            process = new Process(process.getId(), currentRecord.getTime(), process.getName());
            process.setCommandLine(commandLine);

            ProcessDataType processType = new ProcessDataType(process, topFields);
            data.addType(processType);
            data.addProcess(process);

            // remove the data associated with the old process
            double[] data = currentRecord.getData(oldProcessType);
            currentRecord.removeData(oldProcessType);

            // reassociate it with the correct one
            currentRecord.addData(processType, data);
        }
        else {
            LOGGER.debug("command line for process {} reused at line {}", process, in.getLineNumber());
        }
    }

    private DataType buildDataType(String[] values) {
        if (values.length < 3) {
            // Linux disk groups usually are not defined; no need for spurious error output
            if (!values[0].startsWith("DG")) {
                LOGGER.warn("invalid data type definition, no fields defined" + " at line {} for data {}",
                        in.getLineNumber(), java.util.Arrays.toString(values));
            }

            return null;
        }

        if ("ERROR".equals(values[0])) {
            LOGGER.warn("not creating ERROR data type" + " at line {} for data {}", in.getLineNumber(),
                    java.util.Arrays.toString(values));
            return null;
        }

        String id = DataHelper.newString(values[0]);

        if ("".equals(id)) {
            LOGGER.warn("not creating data type with empty id" + " at line {} for data {}", in.getLineNumber(),
                    java.util.Arrays.toString(values));
            return null;
        }
        // the type name may contain the hostname, remove it if so
        String name = values[1];
        int idx = name.indexOf(data.getHostname());

        if (idx != -1) {
            // idx - 1 => assume 'name hostname'; remove space
            name = DataHelper.newString(name.substring(0, idx - 1));
        }
        else {
            name = DataHelper.newString(name);
        }

        List<Integer> toSkip = TYPE_SKIP_INDEXES.get(id);

        if (toSkip == null) {
            toSkip = java.util.Collections.emptyList();
        }

        // 2 => skip data type & timestamp
        String[] fieldNames = new String[values.length - 2 - toSkip.size()];

        for (int i = 2, n = 0; i < values.length; i++) {
            if (toSkip.contains(i)) {
                continue;
            }

            fieldNames[n++] = DataHelper.newString(values[i]);
        }

        for (DataTransform transform : transforms) {
            if (transform.isValidFor(id, null)) {
                return transform.buildDataType(id, null, name, fieldNames);
            }
        }

        // no transform, return as-is
        return new DataType(id, name, fieldNames);
    }

    // process CPU can be > 100, so normalize based on the number of CPUs
    private double[] scaleProcessDataByCPUs(ProcessDataType processType, double[] values) {
        // use the cpu count from the file if no data is available at a given time
        double CPUs = fileCPUs;

        if (isAIX) {
            DataType lpar = data.getType("LPAR");
            if (currentRecord.hasData(lpar)) {
                CPUs = currentRecord.getData(lpar, "entitled");
            }
            else {
                DataType cpuAll = data.getType("PCPU_ALL");

                // hasData should also cover cpuAll == null
                if (currentRecord.hasData(cpuAll)) {
                    CPUs = currentRecord.getData(cpuAll, "Entitled Capacity");
                }
            }
        }
        else {
            DataType cpuAll = data.getType("CPU_ALL");

            if (currentRecord.hasData(cpuAll)) {
                CPUs = (int) currentRecord.getData(cpuAll, "CPUs");
            }
        }

        if (CPUs > 1) {
            for (String field : processType.getFields()) {
                if (field.startsWith("%")) {
                    // assume %CPU, %Usr, %Sys or %Wait
                    values[processType.getFieldIndex(field)] /= CPUs;
                }
            }
        }

        return values;
    }

    private void completeCurrentRecord() {
        for (DataPostProcessor processor : processors) {
            processor.postProcess(data, currentRecord);
        }

        data.addRecord(currentRecord);

        currentRecord = null;
    }

    private static final Map<String, List<Integer>> TYPE_SKIP_INDEXES;

    static {
        Map<String, List<Integer>> tempIndexes = new java.util.HashMap<String, List<Integer>>();

        tempIndexes.put("RAWLPAR", java.util.Collections.unmodifiableList(java.util.Arrays.asList(2, 3)));
        tempIndexes.put("RAWCPUTOTAL", java.util.Collections.singletonList(4));

        TYPE_SKIP_INDEXES = java.util.Collections.unmodifiableMap(tempIndexes);
    }
}
