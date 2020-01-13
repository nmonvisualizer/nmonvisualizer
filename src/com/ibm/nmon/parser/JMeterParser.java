package com.ibm.nmon.parser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.IIOException;

import com.ibm.nmon.analysis.AnalysisRecord;
import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.util.DataHelper;
import com.ibm.nmon.util.FileHelper;

public final class JMeterParser {
    private static final Pattern DATA_SPLITTER = Pattern.compile(",");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z");
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.000");

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: JMeterParser <directory>");
            return;
        }

        File pathToParse = new File(args[0]);
        List<String> toParse = new java.util.ArrayList<String>();

        FileHelper.recurseDirectories(java.util.Collections.singletonList(pathToParse), new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".csv");
            }
        }, toParse);

        if (toParse.size() == 0) {
            if (pathToParse.isDirectory()) {
                System.err.println("no files to parse in " + pathToParse);
            } else {
                System.err.println(pathToParse + " is not a recognized file");
            }
            return;
        }

        Map<String, StringBuilder> buffers = new java.util.TreeMap<String, StringBuilder>();

        int n = 0;
        for (String file : toParse) {
            if (n > 10) {
                break;
            }
            //n++;
            System.out.print("Parsing file " + file + "... ");
            System.out.flush();

            try {
                Long start = System.nanoTime();
                BasicDataSet data = parse(file);
                AnalysisRecord analysis = new AnalysisRecord(data);

                outputData("By GPID", analysis, buffers);
                outputData("By Phone", analysis, buffers);
                outputData("By Email", analysis, buffers);

                System.out.println("Complete with " + data.getRecordCount() + " records in "
                        + (System.nanoTime() - start) / 1000000.0d + "ms");
            } catch (Exception e) {
                System.out.println("Failed parsing " + file);
                e.printStackTrace();
            }
        }

        System.out.println();

        for (StringBuilder buffer : buffers.values()) {
            System.out.println(buffer);
        }
    }

    private static final void outputData(String transaction, AnalysisRecord analysis,
            Map<String, StringBuilder> buffers) {
        BasicDataSet data = (BasicDataSet)analysis.getDataSet();
        DataType type = data.getType(transaction);

        if (type == null) { return; }

        String key = data.getMetadata("test") + "|" + transaction;
        StringBuilder buffer = buffers.get(key);

        if (buffer == null) {
            buffer = new StringBuilder(64 * 1024);
            buffer.append("TestName,Datetime,Tx,Average,99%,95%,Median,Min,Max,Count,Error Count,Error%\n");
            buffers.put(key, buffer);
        }

        outputCsv(analysis, type, buffer);
    }

    private static final void outputCsv(AnalysisRecord analysis, DataType type, StringBuilder buffer) {
        BasicDataSet data = (BasicDataSet)analysis.getDataSet();

        int count = analysis.getCount(type, "responseTime");
        int errorCount = Integer.parseInt(data.getMetadata(type.getId() + "_errors"));
        double errorPct = (double)errorCount / count * 100;

        buffer.append(data.getMetadata("test"));
        buffer.append(',');
        buffer.append(data.getMetadata("datetime"));
        buffer.append(',');
        buffer.append(type.getName());
        buffer.append(',');
        buffer.append(NUMBER_FORMAT.format(analysis.getAverage(type, "responseTime")));
        buffer.append(',');
        buffer.append(NUMBER_FORMAT.format(analysis.get99thPercentile(type, "responseTime")));
        buffer.append(',');
        buffer.append(NUMBER_FORMAT.format(analysis.get95thPercentile(type, "responseTime")));
        buffer.append(',');
        buffer.append(NUMBER_FORMAT.format(analysis.getMedian(type, "responseTime")));
        buffer.append(',');
        buffer.append(NUMBER_FORMAT.format(analysis.getMinimum(type, "responseTime")));
        buffer.append(',');
        buffer.append(NUMBER_FORMAT.format(analysis.getMaximum(type, "responseTime")));
        buffer.append(',');
        buffer.append(count);
        buffer.append(',');
        buffer.append(errorCount);
        buffer.append(',');
        buffer.append(NUMBER_FORMAT.format(errorPct));
        buffer.append("\n");
    }

    private static final BasicDataSet parse(String filename) throws IOException, ParseException {
        BasicDataSet dataSet = new BasicDataSet(filename);

        File file = new File(filename);
        File parent = file.getParentFile();

        if (parent == null) { throw new IOException(filename + " is not in a directory"); }

        dataSet.setMetadata("datetime", parent.getName());
        File grandParent = parent.getParentFile();

        String testName = null;

        if (grandParent == null) {
            testName = "DFS";
        } else {
            testName = grandParent.getName();
        }

        dataSet.setMetadata("test", testName);

        String[] fields = new String[] { "responseTime", "responseCode", "bytes" };
        Map<String, DataType> dataTypes = new java.util.HashMap<String, DataType>(5);

        // hack to store error counts in metadata rather than calculate them again later which would require an
        // additional iteration through all DataTypes in all DataRecords in all DataSets
        Map<String, Integer> errorCounts = new java.util.HashMap<String, Integer>(5);

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line = null;

            // ignore header
            in.readLine();

            while ((line = in.readLine()) != null) {
                String[] data = DATA_SPLITTER.split(line);

                int idx = data[0].indexOf(".");

                if (idx == -1) { throw new IIOException("cannot parse datetime with microseconds '" + data[3] + "'"); }

                idx += 4;

                Date d = DATE_FORMAT.parse(data[0].substring(0, idx) + data[0].substring(idx + 1));
                int micros = Integer.parseInt(data[0].substring(idx, idx + 1)) * 100;

                String transaction = DataHelper.newString(data[2]);

                if ("Read SalesOrder".equals(transaction)) {
                    transaction = "SalesOrder";
                }

                transaction = transaction.intern();

                DataType type = dataTypes.get(transaction);

                if (type == null) {
                    type = new DataType(transaction, transaction, fields);
                    dataSet.addType(type);
                    dataTypes.put(transaction, type);

                    errorCounts.put(transaction, 0);
                }

                double responseTime = Double.parseDouble(data[1]);
                double bytes = Double.parseDouble(data[8]);

                double responseCode = 500;

                try {
                    responseCode = Double.parseDouble(data[3]);
                } catch (NumberFormatException nfe) {
                    // ignore and leave as 500
                }

                // awful hack to increment the time when the data set already contains a record for this time
                // this is definitely possible since JMeter can spit out multiple records in the same microsecond
                // note that this makes the DataRecord's time completely unusable but we are only doing analysis on the
                // data so it does not matter here
                long time = d.getTime() * 1000 + micros;

                while (dataSet.getRecord(time) != null) {
                    ++time;
                }

                DataRecord record = new DataRecord(time, DataHelper.newString(data[0]));
                record.addData(type, new double[] { responseTime, responseCode, bytes });
                dataSet.addRecord(record);

                if (responseCode > 200) {
                    errorCounts.put(transaction, errorCounts.get(transaction) + 1);
                }
            }
        }

        for (Map.Entry<String, Integer> e : errorCounts.entrySet()) {
            dataSet.setMetadata(e.getKey() + "_errors", e.getValue().toString());
        }

        return dataSet;
    }
}
