package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

import java.io.File;
import java.io.IOException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;

import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.SubDataType;

public final class JSONParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JSONParser.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BasicDataSet data = null;
    private SimpleDateFormat format = null;

    public BasicDataSet parse(File file) throws IOException, ParseException {
        return parse(file.getAbsolutePath());
    }

    public BasicDataSet parse(String filename) throws IOException, JsonParseException {
        long start = System.nanoTime();

        try {
            Map<String, Object> root = MAPPER.readValue(new java.io.File(filename),
                    new TypeReference<Map<String, Object>>() {});

            data = new BasicDataSet(filename);

            Object temp = root.get("hostname");

            if (temp == null) {
                throw new IOException("field 'hostname' not found");
            }

            data.setHostname((String) temp);

            format = parseDateFormat(root.get("whenPattern"), root.get("timezone"));

            parseMetadata(root.get("metadata"));
            parseTypes(root.get("types"));
            parseData(root.get("data"));

            return data;
        }
        finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                        (System.nanoTime() - start) / 1000000.0d);
            }

            data = null;
            format = null;
        }
    }

    private SimpleDateFormat parseDateFormat(Object whenPattern, Object timezone) throws IOException {
        if (whenPattern == null) {
            throw new IOException("field 'whenPattern' not found");
        }

        format = new SimpleDateFormat((String) whenPattern);

        if (timezone != null) {
            if (timezone instanceof Number) {
                Number tz = (Number) timezone;
                format.setTimeZone(new SimpleTimeZone((int) (tz.doubleValue() * 3600000), tz.toString()));
            }
            else if (timezone instanceof String) {
                format.setTimeZone(SimpleTimeZone.getTimeZone((String) timezone));

                if (format.getTimeZone().equals(SimpleTimeZone.getTimeZone("GMT"))) {
                    LOGGER.warn(
                            "'timezone' value defined as '{}' but Java interpreted this as GMT; are you sure this is a valid value?",
                            timezone);
                }
            }
            else {
                LOGGER.warn("timezone '{}' is not a valid format; it must be a number or a String", timezone);
                // return format without a set timezone
            }

            data.setMetadata("timezone", format.getTimeZone().getDisplayName());
        }
        else {
            LOGGER.info("no 'timezone' value defined; defaulting to {}, ({})", format.getTimeZone().getID(), format
                    .getTimeZone().getRawOffset() / 3600000.0);
        }

        return format;
    }

    private void parseMetadata(Object rawMetadata) {
        if (rawMetadata != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) rawMetadata;

            for (String name : metadata.keySet()) {
                data.setMetadata(name, metadata.get(name));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseTypes(Object rawTypes) throws IOException {
        if (rawTypes == null) {
            throw new IOException("'types' must be defined");
        }

        List<Map<String, Object>> types = (List<Map<String, Object>>) rawTypes;

        if (types.size() == 0) {
            throw new IOException("at least one 'type' must be defined");
        }

        for (Map<String, Object> type : types) {
            String typeName = (String) type.get("name");
            String typeId = (String) type.get("id");

            if (typeId == null) {
                LOGGER.warn("typeId must be defined for each type (typeName = '{}'); it will be ignored", typeName);
                continue;
            }

            if (typeName == null) {
                typeName = typeId;
            }

            Object rawFields = type.get("fields");

            if (rawFields == null) {
                LOGGER.warn("no fields defined for type '{}'; it will be ignored", typeId);
                continue;
            }

            List<String> fields = (List<String>) rawFields;

            String[] fieldsArray = new String[fields.size()];

            for (int i = 0; i < fieldsArray.length; i++) {
                fieldsArray[i] = fields.get(i);
            }

            Object temp = type.get("subtypes");

            if (temp != null) {
                List<String> subtypes = (List<String>) temp;

                for (String subtype : subtypes) {
                    DataType dataType = new SubDataType(typeId, subtype, typeName, fieldsArray);
                    data.addType(dataType);
                }
            }
            else {
                DataType dataType = new DataType(typeId, typeName, fieldsArray);
                data.addType(dataType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseData(Object rawData) throws IOException {
        if (rawData == null) {
            throw new IOException("'data' must be defined");
        }

        List<Map<String, Object>> dataToParse = (List<Map<String, Object>>) rawData;

        if ((dataToParse.size() == 0)) {
            throw new IOException("at least one 'data' element must be defined");
        }

        for (Map<String, Object> datum : dataToParse) {
            Object temp = datum.get("when");

            if (temp == null) {
                LOGGER.warn("'when' not defined for data record; it will be ignored. Previous time was '{}'",
                        data.getRecordCount() == 0 ? "<null>" : format.format(new java.util.Date(data.getEndTime())));
                continue;
            }

            long time = 0;
            String timestamp = (String) temp;

            try {
                time = format.parse(timestamp).getTime();
            }
            catch (ParseException pe) {
                LOGGER.warn("cannot parse 'when' value '{}'; the data record will be ignored", temp);
                continue;
            }

            DataRecord record = new DataRecord(time, timestamp);

            for (DataType type : data.getTypes()) {
                parseTypeData(datum, type, record);
            }

            data.addRecord(record);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseTypeData(Map<String, Object> datum, DataType type, DataRecord record) {
        String typeId = type.getId();
        boolean isSubType = false;

        if (type instanceof SubDataType) {
            typeId = ((SubDataType) type).getPrimaryId();
            isSubType = true;
        }

        Object temp = datum.get(typeId);

        if (temp == null) {
            LOGGER.warn("no data for type '{}' at time {}", typeId, format.format(new java.util.Date(record.getTime())));
            return;
        }

        if (isSubType) {
            if (temp instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) temp;

                String subtypeId = ((SubDataType) type).getSubId();
                temp = map.get(subtypeId);

                if (temp == null) {
                    LOGGER.warn("no data for subtype '{}' at time {}", subtypeId,
                            format.format(new java.util.Date(record.getTime())));
                    return;
                }
                else {
                    List<Number> values = (List<Number>) temp;

                    double[] doubleValues = new double[values.size()];

                    for (int i = 0; i < doubleValues.length; i++) {
                        Number value = values.get(i);

                        if (value == null) {
                            doubleValues[i] = Double.NaN;
                        }
                        else {
                            doubleValues[i] = value.doubleValue();
                        }
                    }

                    record.addData(type, doubleValues);
                }
            }
            else {
                LOGGER.warn("unknown JSON object for type '{}' at time {}; it must be an object", type.getId(),
                        format.format(new java.util.Date(record.getTime())));
            }
        }
        else {
            if (temp instanceof List) {
                List<Number> values = (List<Number>) temp;

                double[] doubleValues = new double[values.size()];

                for (int i = 0; i < doubleValues.length; i++) {
                    Number value = values.get(i);

                    if (value == null) {
                        doubleValues[i] = Double.NaN;
                    }
                    else {
                        doubleValues[i] = value.doubleValue();
                    }
                }

                record.addData(type, doubleValues);
            }
            else {
                LOGGER.warn("unknown JSON object for type '{}' at time {}; it must be an array", typeId,
                        format.format(new java.util.Date(record.getTime())));
            }
        }
    }
}
