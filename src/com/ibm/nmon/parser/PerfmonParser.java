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

    // older versions of Windows output CSV without "
    private static final Pattern DATA_SPLITTER = Pattern.compile(",");
    private static final Pattern DATA_SPLITTER_QUOTES = Pattern.compile("\",\"");

    private static final Pattern SUBCATEGORY_SPLITTER = Pattern.compile(":");
    // "\\hostname\category (optional subcategory)\metric"
    // note storing a matcher vs a pattern is _NOT_ thread safe
    // first group is non-greedy (.*?) to allow proper parsing of strings like
    // \\SYSTEM\Paging File(\??\D:\pagefile.sys)\% Usage
    private static final Matcher METRIC_MATCHER = Pattern.compile("\\\\\\\\(.*?)\\\\(.*)\\\\(.*)\"?").matcher("");

    private LineNumberReader in = null;

    private PerfmonDataSet data = null;

    private final WindowsBytesTransform bytesTransform = new WindowsBytesTransform();

    // builders for each column
    private DataTypeBuilder[] buildersByColumn;
    // builders by type id
    private Map<String, DataTypeBuilder> buildersById = new java.util.HashMap<String, DataTypeBuilder>();

    public PerfmonDataSet parse(File file, boolean scaleProcessesByCPU) throws IOException, ParseException {
        return parse(file.getAbsolutePath(), scaleProcessesByCPU);
    }

    public PerfmonDataSet parse(String filename, boolean scaleProcessesByCPU) throws IOException, ParseException {
        long start = System.nanoTime();

        data = new PerfmonDataSet(filename);
        data.setMetadata("OS", "Perfmon");

        try {
            in = new LineNumberReader(new FileReader(filename));

            String line = in.readLine();

            // assume all columns will be quoted if the first one is
            Pattern splitter = null;
            if (line.startsWith("\"")) {
                splitter = DATA_SPLITTER_QUOTES;
            }
            else {
                splitter = DATA_SPLITTER;
            }

            parseHeader(splitter.split(line));

            while ((line = in.readLine()) != null) {
                parseData(splitter.split(line));
            }

            long postProcessStart = System.nanoTime();

            // post process after parsing all the data since DataTypes are built lazily
            WindowsNetworkPostProcessor networkPostProcessor = new WindowsNetworkPostProcessor();
            WindowsProcessPostProcessor processPostProcessor = null;

            networkPostProcessor.addDataTypes(data);

            if (scaleProcessesByCPU) {
                processPostProcessor = new WindowsProcessPostProcessor();
                processPostProcessor.addDataTypes(data);
            }

            for (DataRecord record : data.getRecords()) {
                networkPostProcessor.postProcess(data, record);

                if (scaleProcessesByCPU) {
                    processPostProcessor.postProcess(data, record);
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Post processing" + " complete for {} in {}ms", data.getSourceFile(),
                        (System.nanoTime() - postProcessStart) / 1000000.0d);
            }

            DataHelper.aggregateProcessData(data, LOGGER);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parse" + " complete for {} in {}ms", data.getSourceFile(),
                        (System.nanoTime() - start) / 1000000.0d);
            }

            return data;

        }
        finally {
            in.close();

            data = null;

            // columnTypes.clear();
            buildersById.clear();
            // processes.clear();
            buildersByColumn = null;

            bytesTransform.reset();
        }
    }

    private void parseHeader(String[] header) {
        buildersByColumn = new DataTypeBuilder[header.length];

        // remove trailing " or ,
        String lastData = header[header.length - 1];

        if (lastData.endsWith("\"")) {
            header[header.length - 1] = lastData.substring(0, lastData.length() - 1);
        }
        else if (lastData.endsWith(",")) {
            header[header.length - 1] = lastData.substring(0, lastData.length() - 2);
        }

        // parse out the timezone in a format like (PDH-CSV 4.0) (GMT Daylight Time)(-60)
        int idx = header[0].lastIndexOf('(');

        if (idx == -1) {
            LOGGER.warn("version header '{0}' is not in the right format, the time zone will default to UTC",
                    header[0]);
            TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        else {
            String temp = header[0].substring(idx + 1, header[0].length() - 1);

            try {
                // timezone format in negative minutes from UTC
                double offset = Integer.parseInt(temp) / -60.0d;

                TIMESTAMP_FORMAT.setTimeZone(new java.util.SimpleTimeZone((int) (offset * 3600000), temp));
            }
            catch (NumberFormatException nfe) {
                LOGGER.warn("version header '{0}' is not in the right format, the time zone will default to UTC",
                        header[0]);
                TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
        }

        // timestamp does not belong to a category
        // columnTypes.add(null);
        buildersByColumn[0] = null;

        // read the first column to get the hostname
        METRIC_MATCHER.reset(header[1]);

        if (METRIC_MATCHER.matches()) {
            // assume hostname does not change
            data.setHostname(METRIC_MATCHER.group(1).toLowerCase());
        }
        else {
            throw new IllegalArgumentException("hostname not found in '" + header[1] + "'");
        }

        for (int i = 1; i < header.length; i++) {
            METRIC_MATCHER.reset(header[i]);

            if (!METRIC_MATCHER.matches()) {
                LOGGER.warn("'{}' is not a valid header column", header[i]);
                buildersByColumn[i] = null;
                continue;
            }

            // looking for type id (sub type id)
            String toParse = METRIC_MATCHER.group(2);

            String uniqueId = null;
            String id = null;
            String subId = null;

            idx = toParse.indexOf('(');

            if (idx != -1) { // has sub type
                int endIdx = toParse.indexOf(')', idx + 1);

                if (endIdx == -1) {
                    LOGGER.warn("no end parentheses found in header column '{}'", toParse);
                    // columnTypes.add(null);
                    buildersByColumn[i] = null;
                    continue;
                }
                else {
                    id = DataHelper.newString(toParse.substring(0, idx));
                    subId = DataHelper.newString(parseSubId(id, toParse.substring(idx + 1, endIdx)));
                    uniqueId = SubDataType.buildId(id, subId);
                }
            }
            else {
                id = uniqueId = DataHelper.newString(toParse);
            }

            String field = parseField(id, METRIC_MATCHER.group(3));

            DataTypeBuilder builder = buildersById.get(uniqueId);

            if (builder == null) {
                builder = new DataTypeBuilder(uniqueId, id, subId);
                buildersById.put(uniqueId, builder);
            }

            if (data.getTypeIdPrefix().equals(id)) { // Process
                // skip Total and Idle processes
                if ("Idle".equals(subId) || "Total".equals(subId)) {
                    buildersByColumn[i] = null;
                }
                // skip ID Process field but use is as the process id
                else if ("ID Process".equals(field)) {
                    buildersByColumn[i] = null;
                    builder.setProcessIdColumn(i);
                }
                else {
                    buildersByColumn[i] = builder;
                    builder.addField(field);
                }
            }
            else {
                buildersByColumn[i] = builder;
                builder.addField(field);
            }
        }
    }

    private void parseData(String[] rawData) {
        if (rawData.length != buildersByColumn.length) {
            LOGGER.warn("invalid number of data columns at line {}, this data will be skipped", in.getLineNumber());
            return;
        }

        // remove trailing " or ,
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

        Map<String, DataHolder> dataByType = new java.util.HashMap<String, DataHolder>();

        for (int i = 1; i < rawData.length; i++) {
            DataTypeBuilder builder = buildersByColumn[i];

            if (builder == null) {
                continue;
            }
            else {
                DataHolder holder = dataByType.get(builder.unique);

                if (holder == null) {
                    holder = new DataHolder(builder.fields.size());

                    dataByType.put(builder.unique, holder);
                }

                try {
                    holder.add(parseDouble(rawData[i]));
                }
                catch (NumberFormatException nfe) {
                    LOGGER.warn("invalid double '{}' at line {}, column {}; it will be NaN", rawData[i],
                            in.getLineNumber(), i + 1);
                    holder.add(Double.NaN);
                }
            }
        }

        DataRecord record = new DataRecord(time, timestamp);

        for (String unique : dataByType.keySet()) {
            DataTypeBuilder builder = buildersById.get(unique);
            DataHolder holder = dataByType.get(unique);

            DataType type = builder.build(time, rawData);

            double[] values = holder.data;

            if (bytesTransform.isValidFor(builder.id, builder.subId)) {
                if (type.hasField("% Used Space")) {
                    int idx = type.getFieldIndex("% Used Space");

                    values[idx] = 100 - values[idx];
                }

                values = bytesTransform.transform(type, values);
            }

            record.addData(type, values);
        }

        data.addRecord(record);
    }

    private String parseSubId(String id, String toParse) {
        // some ESXTop data need special handling
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
        if (value.isEmpty() || (value.charAt(0) == ' ')) {
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

    // builder class for DataTypes
    // needed due to Perfmon interleaving Process data columns
    // Processes also need to be created with a start time and pid are unknown until data is parsed
    private final class DataTypeBuilder {
        // id + subId, used for hashCode and equals
        private final String unique;

        private final String id;
        private final String subId;

        // possible column mapping for ID Process column
        private int processIdColumn = -1;

        private final List<String> fields = new java.util.ArrayList<String>();

        private DataType type;

        DataTypeBuilder(String unique, String id, String subId) {
            this.unique = unique;

            this.id = id;
            this.subId = subId;
        }

        void addField(String field) {
            // assume no duplicates will happen
            fields.add(field);
        }

        void setProcessIdColumn(int processIdColumn) {
            this.processIdColumn = processIdColumn;
        }

        @Override
        public int hashCode() {
            return unique.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return unique.equals(o);
        }

        DataType build(long startTime, String[] rawData) {
            if (type != null) {
                return type;
            }

            String[] fieldsArray = new String[fields.size()];
            fields.toArray(fieldsArray);

            if (data.getTypeIdPrefix().equals(id)) { // Process
                int pid = (int) (processIdColumn != -1 ? parseDouble(rawData[processIdColumn]) : 0);
                String processName = subId; // store processes with full name

                // parse out pid, if available via
                // HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\PerfProc\Performance
                // ProcessNameFormat=2
                int idx = processName.indexOf('_');

                if (idx != -1) {
                    String temp = processName.substring(idx + 1, processName.length());

                    try {
                        pid = Integer.parseInt(temp);
                        processName = DataHelper.newString(processName.substring(0, idx));
                    }
                    catch (NumberFormatException nfe) {
                        // process name might contain _
                        // ignore and continue parsing pid using other methods
                    }
                }

                idx = processName.indexOf('#');

                if (idx != -1) {
                    processName = DataHelper.newString(processName.substring(0, idx));
                }

                if (pid == 0) {
                    // artificial process id
                    pid = data.getProcessCount() + 1;
                }

                Process process = new Process(pid, startTime, processName, data.getTypeIdPrefix() + " " + processName);
                data.addProcess(process);

                type = new ProcessDataType(process, fieldsArray);
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

                    type = bytesTransform.buildDataType(id, subId, name, fieldsArray);
                }
                else {
                    if (subId == null) {
                        type = new DataType(id, name, fieldsArray);
                    }
                    else {
                        type = new SubDataType(id, subId, name, fieldsArray);
                    }
                }
            }

            data.addType(type);
            return type;
        }
    }

    // simple holder for field data as it is being read
    private final class DataHolder {
        private int nextIdx = 0;
        private final double[] data;

        DataHolder(int size) {
            data = new double[size];
        }

        void add(double d) {
            data[nextIdx++] = d;
        }
    }
}
