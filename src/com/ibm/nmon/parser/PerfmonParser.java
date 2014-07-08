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

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                        (System.nanoTime() - start) / 1000000.0d);
            }

            scaleProcessDataByCPUs();

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
                String name = null;
                String subId = null;
                String typeId = null;

                int idx = toParse.indexOf('(');

                if (idx != -1) {
                    int endIdx = toParse.indexOf(')', idx + 1);

                    if (endIdx == -1) {
                        LOGGER.warn("no end parentheses found in '{}' at line {}", toParse, in.getLineNumber());
                        columnTypes.add(null);
                        continue;
                    }
                    else {
                        name = DataHelper.newString(toParse.substring(0, idx));
                        subId = DataHelper.newString(parseSubId(name, toParse.substring(idx + 1, endIdx)));
                        typeId = SubDataType.buildId(name, subId);
                    }
                }
                else {
                    name = typeId = DataHelper.newString(toParse);
                }

                DataTypeBuilder builder = builders.get(typeId);

                if (builder == null) {
                    builder = new DataTypeBuilder();

                    builder.setId(name);
                    builder.setSubId(subId);
                    builder.setName(name);

                    builders.put(typeId, builder);
                }

                builder.addField(parseField(name, subId, typeId, METRIC_MATCHER.group(3)));
                columnTypes.add(typeId);
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

        String lastTypeId = null;
        int n = 0;

        double[] values = null;

        for (int i = 1; i < rawData.length; i++) {
            String typeId = columnTypes.get(i);

            if (typeId == null) {
                continue;
            }
            else if (typeId.equals(lastTypeId)) { // use current DataType
                values[n++] = parseDouble(rawData[i]);
            }
            else { // create a new DataType
                if (lastTypeId != null) {
                    DataType lastType = getDataType(lastTypeId, in.getLineNumber());

                    if (lastType instanceof ProcessDataType) {
                        ((ProcessDataType) lastType).getProcess().setEndTime(currentRecord.getTime());
                    }

                    currentRecord.addData(lastType, values);
                }

                DataType type = getDataType(typeId, in.getLineNumber());

                values = new double[type.getFieldCount()];
                values[0] = parseDouble(rawData[i]);

                n = 1;
            }

            lastTypeId = typeId;
        }

        // add final data type
        currentRecord.addData(getDataType(lastTypeId, in.getLineNumber()), values);
    }

    private String parseSubId(String id, String toParse) {
        if ("Interrupt Vector".equals(id)) {
            String[] split = SUBCATEGORY_SPLITTER.split(toParse);

            // interrupt id
            return split[0];
        }
        else if (id.startsWith("Group")) {
            // remove leading process id
            return toParse.substring(toParse.indexOf(':') + 1, toParse.length());
        }
        else if ("Vcpu".equals(id)) {
            // remove leading process id
            return toParse.substring(toParse.indexOf(':') + 1, toParse.length());
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

    private String parseField(String id, String subId, String unparsedSubId, String toParse) {
        if ("Interrupt Vector".equals(id)) {
            String[] split = SUBCATEGORY_SPLITTER.split(unparsedSubId);

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

    // lazily build DataTypes; ensure Process data has the correct name
    private DataType getDataType(String typeId, int lineNumber) {
        if (typeId.startsWith("Process (")) {
            String processName = typeId.substring((data.getTypeIdPrefix() + " (").length(), typeId.length() - 1);
            Process process = processes.get(processName);

            if (process == null) {
                DataTypeBuilder builder = builders.get(typeId);

                if (builder == null) {
                    throw new IllegalStateException("no DataTypeBuilder for '" + typeId + "' at line " + lineNumber);
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
                    throw new IllegalStateException("no DataTypeBuilder for '" + typeId + "' at line " + lineNumber);
                }

                type = builder.build();
                data.addType(type);
            }

            return type;
        }
    }

    private void scaleProcessDataByCPUs() {
        // get all CPUs
        List<DataType> processors = new java.util.ArrayList<DataType>(8);

        for (DataType type : data.getTypes()) {
            if (type.getName().equals("Processor")) {
                if (!((SubDataType) type).getSubId().equals("Total")) {
                    processors.add(type);
                }
            }
        }

        for (DataRecord record : data.getRecords()) {
            // for each record, count the number of active CPUs
            int processorCount = 0;

            for (DataType processorType : processors) {
                if (record.hasData(processorType)) {
                    ++processorCount;
                }
            }

            // scale all the process data by the number of CPUs
            for (Process process : data.getProcesses()) {
                DataType processType = data.getType(process);

                if (record.hasData(processType)) {
                    double[] data = record.getData(processType);

                    data[processType.getFieldIndex("% Processor Time")] /= processorCount;
                    data[processType.getFieldIndex("% User Time")] /= processorCount;
                }
            }
        }
    }

    private final class DataTypeBuilder {
        private String id = null;
        private String subId = null;
        private String name = null;
        private final List<String> fields = new java.util.ArrayList<String>();

        void setId(String id) {
            this.id = id;
        }

        void setSubId(String subId) {
            this.subId = subId;
        }

        void setName(String name) {
            this.name = name;
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
                Process process = new Process(currentPid++, currentRecord.getTime(), subId, data.getTypeIdPrefix());
                processes.put(subId, process);
                data.addProcess(process);

                return new ProcessDataType(process, fieldsArray);
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
