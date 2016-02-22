package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import java.io.IOException;

import java.io.File;

import java.io.LineNumberReader;

import java.util.regex.Pattern;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;

import com.ibm.nmon.util.DataHelper;

/**
 * Parser for zpool's iostat command. Will parse the data from <code>zpool iostat SAN_ZPOOL -vTd</code>.
 */
public final class ZPoolIOStatParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ZPoolIOStatParser.class);

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    private static final Pattern DATA_SPLITTER = Pattern.compile("\\s+");

    private static final int EXPECTED_DATA_TYPES = 6;

    public static final String DEFAULT_HOSTNAME = "zpool";

    private LineNumberReader in = null;

    private BasicDataSet data = null;

    public BasicDataSet parse(File file) throws IOException, ParseException {
        return parse(file.getAbsolutePath());
    }

    public BasicDataSet parse(String filename) throws IOException, ParseException {
        long start = System.nanoTime();

        data = new BasicDataSet(filename);
        data.setHostname(DEFAULT_HOSTNAME);

        // DataType type = new DataType(id, name, fields)

        // create a sub data type for each pool, mirror, disk, etc
        List<DataType> types = new java.util.ArrayList<DataType>(EXPECTED_DATA_TYPES);

        // track all disks seen and assign unique names
        List<String> diskNames = new java.util.ArrayList<String>(8);
        Map<String, Integer> nameCounts = new java.util.HashMap<String, Integer>(8);

        String line = null;

        try {
            in = new LineNumberReader(new java.io.FileReader(filename));

            while ((line = in.readLine()) != null) {
                long time = TIMESTAMP_FORMAT.parse(line).getTime();

                DataRecord record = new DataRecord(time, line);

                // header lines
                discardLine();
                discardLine();
                // ---- separator line
                discardLine();

                // used to hold all the values for a data record so they can be transposed from disk (row) / metric
                // (column) to metric (data type) / disk (field)
                List<List<Double>> values = new java.util.ArrayList<List<Double>>(EXPECTED_DATA_TYPES);

                for (int i = 0; i < EXPECTED_DATA_TYPES; i++) {
                    values.add(new java.util.ArrayList<Double>(8));
                }

                while ((line = in.readLine()) != null) {
                    if (line.startsWith("-")) {
                        break;
                    }

                    line = line.trim();
                    String[] data = DATA_SPLITTER.split(line);

                    if (types.isEmpty()) { // first record; parse out disk names and use those as fields
                        String disk = data[0].trim();

                        int n = 1;

                        // name already seen, add the counter
                        if (nameCounts.containsKey(disk)) {
                            n = nameCounts.get(disk) + 1;
                            disk += n;
                        }

                        diskNames.add(DataHelper.newString(disk));
                        nameCounts.put(disk, n);
                    }

                    // transpose values so each data type will have the data for all disks
                    values.get(0).add(parseValue(data[1], 1));
                    values.get(1).add(parseValue(data[2], 2));
                    values.get(2).add(parseValue(data[3], 3));
                    values.get(3).add(parseValue(data[4], 4));
                    values.get(4).add(parseValue(data[5], 5));
                    values.get(5).add(parseValue(data[6], 6));
                }

                // first record complete => all disk names are available to create the types
                if (types.isEmpty()) {
                    nameCounts = null;

                    if (diskNames.isEmpty()) {
                        throw new IOException("no disks defined in the first data record");
                    }

                    LOGGER.trace("found {} disks: {}", diskNames.size(), diskNames);

                    String[] fields = new String[diskNames.size()];
                    diskNames.toArray(fields);

                    types.add(new SubDataType("IOStat ZPool", "alloc", "Capacity Allocated", false, fields));
                    types.add(new SubDataType("IOStat ZPool", "free", "Capacity Free", false, fields));
                    types.add(new SubDataType("IOStat ZPool", "r/s", "reads " + "per second", false, fields));
                    types.add(new SubDataType("IOStat ZPool", "w/s", "writes " + "per second", false, fields));
                    types.add(new SubDataType("IOStat ZPool", "rMB/s", "MB read " + "per second", false, fields));
                    types.add(new SubDataType("IOStat ZPool", "wMB/s", "MB write " + "per second", false, fields));

                    for (DataType type : types) {
                        data.addType(type);
                    }

                    diskNames = null;
                }

                // record complete, add the transposed values to each type
                for (int i = 0; i < types.size(); i++) {
                    List<Double> data = values.get(i);
                    double[] temp = new double[data.size()];

                    for (int j = 0; j < data.size(); j++) {
                        temp[j] = data.get(j);
                    }

                    record.addData(types.get(i), temp);
                }

                // blank record separator line
                discardLine();

                data.addRecord(record);
            }

            return data;
        }
        finally

        {
            if (in != null) {
                try {
                    in.close();
                }
                catch (Exception e) {
                    // ignore
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                            (System.nanoTime() - start) / 1000000.0d);
                }

                in = null;
                data = null;

                types = null;
            }
        }
    }

    private void discardLine() throws IOException {
        String line = in.readLine();

        if (line == null) {
            LOGGER.warn("unexpected end of file at line {}; data may be incomplete");
        }
    }

    private double parseValue(String value, int column) {
        if ("-".equals(value)) {
            return Double.NaN;
        }

        // operations in thousands, everything else in MB
        int multiplier = 1024;

        if ((column == 3) || (column == 4)) {
            multiplier = 1000;
        }

        // if the last char is not a number, get the power of 1024 / 1000 it represents
        char last = value.charAt(value.length() - 1);
        int power = 0;

        switch (last) {
        case 'K':
            power = 1;
            value = value.substring(0, value.length() - 1);
            break;
        case 'M':
            power = 2;
            value = value.substring(0, value.length() - 1);
            break;
        case 'G':
            power = 3;
            value = value.substring(0, value.length() - 1);
            break;
        case 'T':
            power = 4;
            value = value.substring(0, value.length() - 1);
            break;
        case 'P':
            power = 5;
            value = value.substring(0, value.length() - 1);
            break;
        case 'E':
            power = 6;
            value = value.substring(0, value.length() - 1);
            break;
        case 'Z':
            power = 7;
            value = value.substring(0, value.length() - 1);
            break;
        case 'Y':
            power = 8;
            value = value.substring(0, value.length() - 1);
            break;
        // default, do nothing
        }

        try {
            double toReturn = Double.parseDouble(value) * Math.pow(multiplier, power);

            // capacity in GB
            if ((column == 1) || (column == 2)) {
                toReturn /= 1024 * 1024 * 1024;
            }
            // bandwidth in MB
            if ((column == 5) || (column == 6)) {
                toReturn /= 1024 * 1024;
            }

            return toReturn;
        }
        catch (NumberFormatException nfe) {
            LOGGER.warn("invalid numeric data '{}' at line {}, column {}", value, in.getLineNumber(), column);

            return Double.NaN;
        }
    }
}
