package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.util.Map;

import java.io.IOException;

import java.io.File;

import java.io.LineNumberReader;
import java.io.FileReader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;

import com.ibm.nmon.util.DataHelper;

/**
 * A parser for FIO output. This class assumes log files with names in the form of <code>id_datetime_type.log</code>.
 */
public final class FIOParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FIOParser.class);

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyddMM_HHmmss");

    private static final Pattern DATA_SPLITTER = Pattern.compile(",\\s?");

    private static final Map<String, String> TYPE_NAMES;

    static {
        Map<String, String> temp = new java.util.HashMap<String, String>(5);

        temp.put("LAT", "Latency");
        temp.put("CLAT", "Completion Latency");
        temp.put("SLAT", "Submit Latency");
        temp.put("IOPS", "IO Operations");
        temp.put("BW", "Bandwidth");

        TYPE_NAMES = java.util.Collections.unmodifiableMap(temp);
    }

    public BasicDataSet parse(File file, TimeZone timeZone) throws IOException, ParseException {
        return parse(file.getAbsolutePath(), timeZone);
    }

    public BasicDataSet parse(String filepath, TimeZone timeZone) throws IOException {
        File file = new File(filepath);
        String filename = file.getName();

        int end = filename.indexOf("_");

        if (end == -1) {
            throw new IllegalArgumentException(
                    "filename must be formatted as 'identifier_" + "yyyyddMM_HHmmss" + "_type.log'");
        }

        String id = DataHelper.newString(filename.substring(0, end));

        // find second _ for timestamp
        int start = end + 1;
        end = filename.indexOf("_", start);
        end = filename.indexOf("_", end + 1);

        if (end == -1) {
            throw new IllegalArgumentException(
                    "filename must be formatted as 'identifier_" + "yyyyddMM_HHmmss" + "_type.log'");
        }

        String timestamp = DataHelper.newString(filename.substring(start, end));
        long baseTime = 0;

        try {
            baseTime = TIMESTAMP_FORMAT.parse(timestamp).getTime();
        }
        catch (ParseException e) {
            throw new IllegalArgumentException(
                    "timestamp '" + timestamp + "' must be in " + "yyyyddMM_HHmmss" + " format");
        }

        BasicDataSet data = new BasicDataSet(filename);
        data.setHostname(id);
        data.setMetadata("timestamp", timestamp);

        // -4 since filename must end in .fio
        String typeName = DataHelper.newString(filename.substring(end + 1, filename.length() - 4).toUpperCase());

        if (!TYPE_NAMES.containsKey(typeName)) {
            throw new IllegalArgumentException(
                    "unrecoginized type name '" + typeName + "' ; valid values are " + TYPE_NAMES.keySet());
        }

        LineNumberReader in = null;

        try {
            in = new LineNumberReader(new FileReader(file));

            String line = null;
            DataRecord currentRecord = null;

            // usually only 1 block size
            // one aggregator for read, one for write
            Map<String, DataType> typesByBlockSize = new java.util.HashMap<String, DataType>(1);
            Map<String, Aggregator> readDataByBlockSize = new java.util.HashMap<String, Aggregator>(1);
            Map<String, Aggregator> writeDataByBlockSize = new java.util.HashMap<String, Aggregator>(1);

            while ((line = in.readLine()) != null) {
                String[] values = DATA_SPLITTER.split(line);

                if (values.length != 4) {
                    LOGGER.warn("invalid data at line {}; it does not contain 4 fields", in.getLineNumber());
                    continue;
                }

                if (currentRecord == null) {
                    currentRecord = new DataRecord(baseTime + Integer.parseInt(values[0]), values[0]);
                }
                else if (!currentRecord.getTimestamp().equals(values[0])) {
                    // timestamp changed, add the data collected so far and start a new record
                    for (String blockSize : typesByBlockSize.keySet()) {
                        DataType type = typesByBlockSize.get(blockSize);
                        Aggregator readAggregator = readDataByBlockSize.get(blockSize);
                        Aggregator writeAggregator = writeDataByBlockSize.get(blockSize);

                        double read = Double.NaN;
                        double write = Double.NaN;

                        if (readAggregator != null) {
                            read = readAggregator.getAverage();
                            readAggregator.clear();
                        }

                        if (writeAggregator != null) {
                            write = writeAggregator.getAverage();
                            writeAggregator.clear();
                        }

                        currentRecord.addData(type, new double[] { read, write });
                    }

                    data.addRecord(currentRecord);

                    currentRecord = new DataRecord(baseTime + Integer.parseInt(values[0]), values[0]);
                }
                // else continue aggregating data for the current record

                String operation = values[2]; // 0 => read; 1 => write
                String blockSize = DataHelper.newString(values[3]);

                Aggregator aggregator = null;

                if ("0".equals(operation)) {
                    aggregator = readDataByBlockSize.get(blockSize);

                    if (aggregator == null) {
                        aggregator = new Aggregator();
                        readDataByBlockSize.put(blockSize, aggregator);
                    }
                }
                else {
                    aggregator = writeDataByBlockSize.get(blockSize);

                    if (aggregator == null) {
                        aggregator = new Aggregator();
                        writeDataByBlockSize.put(blockSize, aggregator);
                    }
                }

                DataType type = typesByBlockSize.get(blockSize);

                if (type == null) {
                    type = new SubDataType(typeName, blockSize, TYPE_NAMES.get(typeName), "read", "write");
                    data.addType(type);

                    typesByBlockSize.put(blockSize, type);
                }

                try {
                    aggregator.aggregate(Integer.parseInt(values[1]));
                }
                catch (NumberFormatException nfe) {
                    LOGGER.warn("invalid numeric data '{}' at line {}; ignoring", values[1], in.getLineNumber());
                }
            }

            if (currentRecord != null) {
                data.addRecord(currentRecord);
            }

            return data;
        }
        finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static final class Aggregator {
        private int count = 0;
        private int value = 0;

        private void aggregate(int newValue) {
            ++count;
            value += newValue;
        }

        private double getAverage() {
            if (count == 0) {
                return Double.NaN;
            }
            else {
                return (double) value / count;
            }
        }

        private void clear() {
            count = 0;
            value = 0;
        }
    }
}
