package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.io.IOException;

import java.io.File;
import java.io.LineNumberReader;
import java.io.FileReader;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.List;
import java.util.Map;

import java.util.TimeZone;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.SubDataType;

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

    public BasicDataSet parse(File file) throws IOException, ParseException {
        return parse(file.getAbsolutePath());
    }

    public BasicDataSet parse(String filename) throws IOException, ParseException {
        long start = System.nanoTime();

        LineNumberReader in = new LineNumberReader(new FileReader(filename));

        String line = in.readLine();

        String[] header = DATA_SPLITTER.split(line);

        String lastData = header[header.length - 1];

        if (lastData.endsWith("\"")) {
            header[header.length - 1] = lastData.substring(0, lastData.length() - 1);
        }

        BasicDataSet data = new BasicDataSet(filename);
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

        Map<String, DataTypeBuilder> builders = new java.util.HashMap<String, DataTypeBuilder>();

        // track the DataType for each column
        List<String> columnTypes = new java.util.ArrayList<String>(1000);
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

        for (DataTypeBuilder builder : builders.values()) {
            data.addType(builder.build());
        }

        while ((line = in.readLine()) != null) {
            String[] rawData = DATA_SPLITTER.split(line);

            if (rawData.length != columnTypes.size()) {
                LOGGER.warn("invalid number of data columns at line {}, this data will be skipped", in.getLineNumber());
                continue;
            }

            lastData = rawData[rawData.length - 1];

            if (lastData.endsWith("\"")) {
                rawData[rawData.length - 1] = lastData.substring(0, lastData.length() - 1);
            }
            else if (lastData.endsWith(",")) {
                rawData[rawData.length - 1] = lastData.substring(0, lastData.length() - 2);
            }

            // remove leading " on timestamp
            String timestamp = DataHelper.newString(rawData[0].substring(1));
            long time = TIMESTAMP_FORMAT.parse(timestamp).getTime();

            DataRecord record = new DataRecord(time, timestamp);
            data.addRecord(record);

            String lastTypeId = null;
            int n = 0;

            double[] values = null;

            for (int i = 1; i < rawData.length; i++) {
                String typeId = columnTypes.get(i);

                if (typeId == null) {
                    continue;
                }
                else if (typeId.equals(lastTypeId)) {
                    values[n++] = parseDouble(rawData[i]);
                }
                else {
                    if (lastTypeId != null) {
                        DataType lastType = data.getType(lastTypeId);
                        record.addData(lastType, values);
                    }

                    DataType type = data.getType(typeId);
                    values = new double[type.getFieldCount()];

                    values[0] = parseDouble(rawData[i]);

                    n = 1;
                }

                lastTypeId = typeId;
            }

            // add final data type
            DataType lastType = data.getType(lastTypeId);
            record.addData(lastType, values);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                    (System.nanoTime() - start) / 1000000.0d);
        }

        in.close();

        return data;
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

    private static final class DataTypeBuilder {
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

            if (subId == null) {
                return new DataType(id, name, fieldsArray);
            }
            else {
                return new SubDataType(id, subId, name, fieldsArray);
            }
        }
    }
}
