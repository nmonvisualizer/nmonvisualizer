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

public final class ESXTopParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ESXTopParser.class);

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    static {
        TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UCT"));
    }

    private static final Pattern DATA_SPLITTER = Pattern.compile("\"?,\"");
    private static final Pattern SUBCATEGORY_SPLITTER = Pattern.compile(":");
    // "\\hostname\category (optional subcategory)\metric"
    // note storing a matcher vs a pattern is _NOT_ thread safe
    private static final Matcher METRIC_MATCHER = Pattern.compile("\\\\\\\\(.*)\\\\(.*)\\\\(.*)").matcher("");

    public BasicDataSet parse(File file) throws IOException, ParseException {
        return parse(file.getAbsolutePath());
    }

    public BasicDataSet parse(String filename) throws IOException, ParseException {
        long start = System.nanoTime();

        LineNumberReader in = new LineNumberReader(new FileReader(filename));

        String line = in.readLine();

        String[] header = DATA_SPLITTER.split(line);

        BasicDataSet data = new BasicDataSet(filename);
        data.setMetadata("OS", "VMWare ESX");

        // read the first column to get the hostname
        for (int i = 1; i < header.length; i++) {
            METRIC_MATCHER.reset(header[i]);

            if (METRIC_MATCHER.matches()) {
                // assume hostname does not change
                data.setHostname(METRIC_MATCHER.group(1));

                break;
            }
        }

        Map<String, DataTypeBuilder> builders = new java.util.HashMap<String, DataTypeBuilder>(TYPE_IDS.size());

        // track the DataType for each column
        List<String> columnTypes = new java.util.ArrayList<String>(5000);
        // timestamp does not belong to a category
        columnTypes.add(null);

        for (int i = 1; i < header.length; i++) {
            METRIC_MATCHER.reset(header[i]);

            if (METRIC_MATCHER.matches()) {
                // looking for type id (sub type id)
                String toParse = METRIC_MATCHER.group(2);
                String name = null;
                String id = null;
                String subId = null;

                String unparsedSubId = null;
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
                        id = TYPE_IDS.get(name);

                        if (id == null) {
                            columnTypes.add(null);
                            continue;
                        }

                        unparsedSubId = toParse.substring(idx + 1, endIdx);
                        subId = DataHelper.newString(parseSubId(id, unparsedSubId));
                        typeId = SubDataType.buildId(id, subId);
                    }
                }
                else {
                    // assume toParse was created from a substring / pattern match so call
                    // newString()
                    name = DataHelper.newString(toParse);
                    id = typeId = TYPE_IDS.get(name);

                    if (id == null) {
                        columnTypes.add(null);
                        continue;
                    }
                }

                DataTypeBuilder builder = builders.get(typeId);

                if (builder == null) {
                    builder = new DataTypeBuilder();

                    builder.setId(id);
                    builder.setSubId(subId);
                    builder.setName(name);

                    builders.put(typeId, builder);
                }

                builder.addField(parseField(id, subId, unparsedSubId, METRIC_MATCHER.group(3)));
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

            String lastData = rawData[rawData.length - 1];

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
                    values[n++] = Double.parseDouble(rawData[i]);
                }
                else {
                    if (lastTypeId != null) {
                        DataType lastType = data.getType(lastTypeId);
                        record.addData(lastType, values);
                    }

                    DataType type = data.getType(typeId);

                    values = new double[type.getFieldCount()];
                    values[0] = Double.parseDouble(rawData[i]);

                    n = 1;
                }

                lastTypeId = typeId;
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                    (System.nanoTime() - start) / 1000000.0d);
        }

        in.close();

        return data;
    }

    private String parseSubId(String id, String toParse) {
        if ("INTERRUPT".equals(id)) {
            String[] split = SUBCATEGORY_SPLITTER.split(toParse);

            // interrupt id
            return split[0];
        }
        else if (id.startsWith("GROUP")) {
            // remove leading process id
            return toParse.substring(toParse.indexOf(':') + 1, toParse.length());
        }
        else if ("VCPU".equals(id)) {
            // remove leading process id
            return toParse.substring(toParse.indexOf(':') + 1, toParse.length());
        }
        else if ("CPU".equals(id)) {
            if (toParse.charAt(0) == '_') {
                // _Total = Total
                return toParse.substring(1);
            }
            else {
                return toParse;
            }
        }
        else {
            return toParse;
        }
    }

    private String parseField(String id, String subId, String unparsedSubId, String toParse) {
        if ("INTERRUPT".equals(id)) {
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

    private static final Map<String, String> TYPE_IDS;

    static {
        Map<String, String> temp = new java.util.HashMap<String, String>();

        temp.put("Group Cpu", "GROUPCPU");
        temp.put("Group Memory", "GROUPMEM");

        temp.put("Interrupt Vector", "INTERRUPT");

        temp.put("Memory", "MEM");
        temp.put("Numa Node", "NUMA");

        temp.put("Physical Cpu", "CPU");

        temp.put("Network Port", "NET");
        temp.put("Vcpu", "VCPU");

        temp.put("Virtual Disk", "VDISK");
        temp.put("Physical Disk", "DISKPHY");
        temp.put("Physical Disk Adapter", "DISKADPT");
        temp.put("Physical Disk Partition", "DISKPART");
        temp.put("Physical Disk Path", "DISKPATH");
        temp.put("Physical Disk Per-Device-Per-World", "DISKWORLD");
        temp.put("Physical Disk SCSI Device", "DISKSCSI");

        temp.put("Power", "POWER");

        TYPE_IDS = java.util.Collections.unmodifiableMap(temp);
    }
}
