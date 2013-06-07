package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;

import java.util.regex.Pattern;

import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.util.DataHelper;

public final class HATJParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NMONParser.class);

    private static final Pattern DATA_SPLITTER = Pattern.compile(",");

    public static final String DEFAULT_HOSTNAME = "hatj";

    public BasicDataSet parse(File file) throws IOException {
        return parse(file.getAbsolutePath());
    }

    public BasicDataSet parse(String filename) throws IOException {
        long start = System.nanoTime();

        // assume HATJ file is like graph<timeinmillis>.csv
        int idx = filename.lastIndexOf("graph");

        String temp = filename.substring(idx + "graph".length(), filename.length() - ".csv".length());
        long startTime = 0;
        try {
            startTime = Long.parseLong(temp);
        }
        catch (NumberFormatException nfe) {
            LOGGER.warn("no valid start time in the file name, using the current time", filename);
            startTime = System.currentTimeMillis();
        }

        LineNumberReader in = null;

        try {
            in = new LineNumberReader(new java.io.FileReader(filename));
            String line = in.readLine();

            if (line == null) {
                throw new IOException("file '" + filename + "' does not appear to have any data records");
            }

            BasicDataSet data = new BasicDataSet(filename);
            data.setHostname(DEFAULT_HOSTNAME);

            // create DataTypes from header line
            String[] values = DATA_SPLITTER.split(line);

            // data[0] = Duration; data[1] = Throughput; data[2] = Hits/sec; data[3] = # of Users
            String[] fields = new String[values.length - 4];

            for (int i = 0; i < fields.length; i++) {
                fields[i] = DataHelper.newString(values[i + 4]);
            }

            DataType info = new DataType("INFO", "HATJ Test Information", "throughput", "hits", "users");
            DataType response = new DataType("RESP", "HATJ Response Times", fields);

            data.addType(info);
            data.addType(response);

            while ((line = in.readLine()) != null) {
                values = DATA_SPLITTER.split(line);

                long duration = Long.parseLong(values[0]);

                DataRecord record = new DataRecord(startTime + (duration * 1000), DataHelper.newString(values[0]));

                record.addData(info, new double[] { Double.parseDouble(values[1]), Double.parseDouble(values[2]),
                        Double.parseDouble(values[3]) });

                double[] recordData = new double[response.getFieldCount()];
                int n = 0;

                for (int i = 4; i < values.length; i++) {
                    if ("".equals(values[i]) || values[i].contains("nan")) {
                        recordData[n] = Double.NaN;
                    }
                    else {
                        recordData[n] = Double.parseDouble(values[i]);
                    }

                    ++n;
                }

                record.addData(response, recordData);
                data.addRecord(record);
            }

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
                LOGGER.debug("Parse complete for {} in {}ms", filename, (System.nanoTime() - start) / 1000000.0d);
            }
        }

    }
}
