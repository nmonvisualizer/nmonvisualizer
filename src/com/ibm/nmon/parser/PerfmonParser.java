package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.ibm.nmon.data.PerfmonDataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;
import com.ibm.nmon.data.ProcessDataType;
import com.ibm.nmon.data.Process;
import com.ibm.nmon.data.transform.WindowsBytesTransform;
import com.ibm.nmon.data.transform.WindowsNetworkPostProcessor;
import com.ibm.nmon.data.transform.WindowsProcessPostProcessor;
import com.ibm.nmon.util.DataHelper;

public final class PerfmonParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PerfmonParser.class);

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    static {
        TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UCT"));
    }

    private static final Pattern DATA_SPLITTER = Pattern.compile("\"?,\"?");
    private static final Pattern SUBCATEGORY_SPLITTER = Pattern.compile(":");
    // "\\hostname\category (optional subcategory)\metric"
    // note storing a matcher vs a pattern is _NOT_ thread safe
    // first group is non-greedy (.*?) to allow proper parsing of strings like
    // \\SYSTEM\Paging File(\??\D:\pagefile.sys)\% Usage
    private static final Matcher METRIC_MATCHER = Pattern.compile("\\\\\\\\(.*?)\\\\(.*)\\\\(.*)\"?").matcher("");

    private LineNumberReader in = null;

    private PerfmonDataSet data = null;
    private DataRecord currentRecord = null;

    // artificial process id; incremented for each Process parsed
    private int currentPid = 1;

    private final WindowsBytesTransform bytesTransform = new WindowsBytesTransform();

    private final List<String> columnTypes = new java.util.ArrayList<String>(1000);
    private final Map<String, DataTypeBuilder> builders = new java.util.HashMap<String, DataTypeBuilder>();
    private final Map<String, Process> processes = new java.util.HashMap<String, Process>();

    public PerfmonDataSet parse(File file) throws IOException, ParseException {
        return parse(file.getAbsolutePath());
    }

    public PerfmonDataSet parse(String filename) throws IOException, ParseException {
        long start = System.nanoTime();

        data = new PerfmonDataSet(filename);

        try {
            in = new LineNumberReader(new FileReader(filename));

            String line = in.readLine();

            parseHeader(DATA_SPLITTER.split(line));

            while ((line = in.readLine()) != null) {
                parseData(DATA_SPLITTER.split(line));
            }

            // post process after parsing all the data since DataTypes are built lazily
            WindowsNetworkPostProcessor networkPostProcessor = new WindowsNetworkPostProcessor();
            WindowsProcessPostProcessor processPostProcessor = new WindowsProcessPostProcessor();

            networkPostProcessor.addDataTypes(data);
            processPostProcessor.addDataTypes(data);

            for (DataRecord record : data.getRecords()) {
                networkPostProcessor.postProcess(data, record);
                processPostProcessor.postProcess(data, record);
            }

            DataHelper.aggregateProcessData(data, LOGGER);

            scaleProcessDataByCPUs();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                        (System.nanoTime() - start) / 1000000.0d);
            }

            return data;

        }
        finally {
            in.close();

            data = null;
            currentRecord = null;
            currentPid = 1;

            columnTypes.clear();
            builders.clear();
            processes.clear();

            bytesTransform.reset();
        }
    }

    private void parseHeader(String[] header) {
        String lastData = header[header.length - 1];

        if (lastData.endsWith("\"")) {
            header[header.length - 1] = lastData.substring(0, lastData.length() - 1);
        }

        data.setMetadata("OS", "Perfmon");

        // read the first column to get the hostname
        for (int i = 1; i < header.length; i++) {
            METRIC_MATCHER.reset(header[i]);

            if (METRIC_MATCHER.matches()) {
                // assume hostname does not change
                data.setHostname(METRIC_MATCHER.group(1));

                break;
            }
        }

        // timestamp does not belong to a category
        columnTypes.add(null);

        for (int i = 1; i < header.length; i++) {
            METRIC_MATCHER.reset(header[i]);

            if (METRIC_MATCHER.matches()) {
                // looking for type id (sub type id)
                String toParse = METRIC_MATCHER.group(2);
                String uniqueId = null;
                String id = null;
                String subId = null;

                int idx = toParse.indexOf('(');

                if (idx != -1) {
                    int endIdx = toParse.indexOf(')', idx + 1);

                    if (endIdx == -1) {
                        LOGGER.warn("no end parentheses found in heade column '{}'", toParse);
                        columnTypes.add(null);
                        continue;
                    }
                    else {
                        id = DataHelper.newString(toParse.substring(0, idx));
                        subId = DataHelper.newString(parseSubId(id, toParse.substring(idx + 1, endIdx)));
                        uniqueId = SubDataType.buildId(id, subId);

                        // skip Process data for Total and Idle
                        if ("Process".equals(id) && ("Idle".equals(subId) || "Total".equals(subId))) {
                            columnTypes.add(null);
                            continue;
                        }
                    }
                }
                else {
                    id = uniqueId = DataHelper.newString(toParse);
                }

                DataTypeBuilder builder = builders.get(uniqueId);

                if (builder == null) {
                    builder = new DataTypeBuilder();

                    builder.setId(id);
                    builder.setSubId(subId);

                    builders.put(uniqueId, builder);
                }

                builder.addField(parseField(id, METRIC_MATCHER.group(3)));
                columnTypes.add(uniqueId);
            }
            else {
                LOGGER.warn("'{}' is not a valid header column", header[i]);
                columnTypes.add(null);
            }
        }
    }

    private void parseData(String[] rawData) {
        if (rawData.length != columnTypes.size()) {
            LOGGER.warn("invalid number of data columns at line {}, this data will be skipped", in.getLineNumber());
            return;
        }

        String lastData = rawData[rawData.length - 1];

        if (lastData.endsWith("\"")) {
            rawData[rawData.length - 1] = lastData.substring(0, lastData.length() - 1);
        }
        else if (lastData.endsWith(",")) {
            rawData[rawData.length - 1] = lastData.substring(0, lastData.length() - 2);
        }

        // remove leading " on timestamp
        String timestamp = DataHelper.newString(rawData[0].substring(1));
        long time = 0;

        try {
            time = TIMESTAMP_FORMAT.parse(timestamp).getTime();
        }
        catch (ParseException pe) {
            LOGGER.warn("invalid timestamp format at line {}, this data will be skipped", in.getLineNumber());
            return;
        }

        currentRecord = new DataRecord(time, timestamp);
        data.addRecord(currentRecord);

        // data for Process is out of order so track the values and array indexes and add to the
        // DataRecord at the end
        Map<String, double[]> valuesById = new java.util.HashMap<String, double[]>();
        Map<String, Integer> indexesById = new java.util.HashMap<String, Integer>();

        for (int i = 1; i < rawData.length; i++) {
            String id = columnTypes.get(i);

            if (id == null) {
                continue;
            }
            else {
                double[] values = valuesById.get(id);

                if (values == null) {
                    DataType type = getDataType(id);

                    values = new double[type.getFieldCount()];
                    int idx = 0;

                    valuesById.put(id, values);
                    indexesById.put(id, idx);
                }

                int idx = indexesById.get(id);

                values[idx] = parseDouble(rawData[i]);

                indexesById.put(id, ++idx);
            }
        }

        for (String typeId : valuesById.keySet()) {
            DataType type = getDataType(typeId);
            double[] data = valuesById.get(typeId);

            // no need to parse id and subid from typeId given that we know the typeId here is built
            // from the type and subtype id and that isValidFor() uses a regex to match
            if (bytesTransform.isValidFor(typeId, null)) {
                if (type.hasField("% Used Space")) {
                    int idx = type.getFieldIndex("% Used Space");

                    data[idx] = 100 - data[idx];
                }

                data = bytesTransform.transform(type, data);
            }

            currentRecord.addData(type, data);
        }
    }

    private String parseSubId(String id, String toParse) {
        if ("Interrupt Vector".equals(id)) {
            String[] split = SUBCATEGORY_SPLITTER.split(toParse);

            // interrupt id
            return split[0];
        }
        else if (id.startsWith("Group")) {
            // remove leading process id
            String[] split = SUBCATEGORY_SPLITTER.split(toParse);

            return split[1];
        }
        else if ("Vcpu".equals(id)) {
            // remove leading process id
            String[] split = SUBCATEGORY_SPLITTER.split(toParse);

            return split[1];
        }
        else if (toParse.charAt(0) == '_') {
            // _Total = Total
            return toParse.substring(1);
        }
        else {
            return toParse;
        }
    }

    private double parseDouble(String value) {
        // assume start with space, whole string is space (i.e. empty)
        if (value.charAt(0) == ' ') {
            return Double.NaN;
        }
        else {
            return Double.parseDouble(value);
        }
    }

    private String parseField(String id, String toParse) {
        if ("Interrupt Vector".equals(id)) {
            String[] split = SUBCATEGORY_SPLITTER.split(toParse);

            // total stats for interrupt
            if (split.length > 1) {
                return DataHelper.newString(split[1]);
            }
            else {
                return toParse;
            }
        }
        else {
            return toParse;
        }
    }

    // lazily build DataTypes since Processes need a start time
    private DataType getDataType(String typeId) {
        // ensure Process data has the correct name
        if (typeId.startsWith("Process (")) {
            String processName = typeId.substring((data.getTypeIdPrefix() + " (").length(), typeId.length() - 1);
            Process process = processes.get(processName);

            if (process == null) {
                DataTypeBuilder builder = builders.get(typeId);

                if (builder == null) {
                    throw new IllegalStateException("no DataTypeBuilder for '" + typeId + "' at line "
                            + in.getLineNumber());
                }

                DataType type = builder.build();
                // builder will create the process;
                data.addType(type);

                return type;
            }
            else {
                return data.getType(process);
            }
        }
        else {
            DataType type = data.getType(typeId);

            if (type == null) {
                DataTypeBuilder builder = builders.get(typeId);

                if (builder == null) {
                    throw new IllegalStateException("no DataTypeBuilder for '" + typeId + "' at line "
                            + in.getLineNumber());
                }

                type = builder.build();
                data.addType(type);
            }

            return type;
        }
    }

    private void scaleProcessDataByCPUs() {
        // get the maximum number of CPUs
        List<DataType> cpuTypes = new java.util.ArrayList<DataType>(8);

        for (DataType type : data.getTypes()) {
            if (type.getId().startsWith("Processor") && !type.getId().contains("Total")) {
                cpuTypes.add(type);
            }
        }

        // if there is more than 1 possible CPU, scale the process data by the CPU count
        if (cpuTypes.size() > 1) {
            long start = System.nanoTime();

            List<ProcessDataType> processTypes = new java.util.ArrayList<ProcessDataType>(data.getProcessCount());

            for (Process process : data.getProcesses()) {
                processTypes.add(data.getType(process));
            }

            for (DataRecord record : data.getRecords()) {
                // CPUs can change dynamically, so recalculate for each record
                int cpuCount = 0;

                for (DataType cpuType : cpuTypes) {
                    if (record.hasData(cpuType)) {
                        ++cpuCount;
                    }
                }

                if (cpuCount > 1) {
                    for (ProcessDataType processType : processTypes) {
                        if (record.hasData(processType)) {
                            for (String field : processType.getFields()) {
                                if (field.startsWith("%")) {
                                    // assume % Processor Time, % User Time or % Privileged Time
                                    record.getData(processType)[processType.getFieldIndex(field)] /= cpuCount;
                                }
                            }
                        }
                    }
                }
            }

            LOGGER.debug("scaling of processes by CPUs complete in {}ms", (System.nanoTime() - start) / 1000000.0d);
        }
    }

    private final class DataTypeBuilder {
        private String id = null;
        private String subId = null;
        private final List<String> fields = new java.util.ArrayList<String>();

        void setId(String id) {
            this.id = id;
        }

        void setSubId(String subId) {
            this.subId = subId;
        }

        void addField(String field) {
            if (!fields.contains(field)) {
                fields.add(field);
            }
        }

        DataType build() {
            if (id == null) {
                return null;
            }

            String[] fieldsArray = new String[fields.size()];
            fields.toArray(fieldsArray);

            if ("Process".equals(id)) {
                int pid = currentPid;
                String processName = subId; // store processes with full name

                // parse out pid, if available via
                // HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\PerfProc\Performance
                // ProcessNameFormat=2
                int idx = subId.indexOf('_');

                if (idx != -1) {
                    String temp = subId.substring(idx + 1, subId.length());

                    try {
                        pid = Integer.parseInt(temp);
                        subId = DataHelper.newString(subId).substring(0, idx);
                    }
                    catch (NumberFormatException nfe) {
                        LOGGER.warn("invalid pid {} at line {}; using {} instead", temp, in.getLineNumber(), pid);

                        // ignore and use currentPid
                        ++currentPid;
                    }
                }
                else {
                    idx = subId.indexOf('#');

                    if (idx != -1) {
                        subId = DataHelper.newString(subId).substring(0, idx);
                    }

                    ++currentPid;
                }

                Process process = new Process(pid, currentRecord.getTime(), subId, data.getTypeIdPrefix());
                processes.put(processName, process);
                data.addProcess(process);

                return new ProcessDataType(process, fieldsArray);
            }
            else {
                String name = SubDataType.buildId(id, subId);

                if (bytesTransform.isValidFor(id, subId)) {
                    // cannot use a DataTransform for disk free -> disk used since disks also need
                    // to have WindowsBytesTransform applied
                    if ("LogicalDisk".equals(id) || "PhysicalDisk".equals(id)) {
                        for (int i = 0; i < fieldsArray.length; i++) {
                            String field = fieldsArray[i];

                            if ("% Free Space".equals(field)) {
                                fieldsArray[i] = "% Used Space";
                            }
                        }
                    }

                    return bytesTransform.buildDataType(id, subId, name, fieldsArray);
                }
                else {
                    if (subId == null) {
                        return new DataType(id, name, fieldsArray);
                    }
                    else {
                        return new SubDataType(id, subId, name, fieldsArray);
                    }
                }
            }
        }
    }
}
