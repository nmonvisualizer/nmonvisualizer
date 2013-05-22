package com.ibm.nmon.util;

import java.util.List;

import java.io.IOException;
import java.io.Writer;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.chart.data.DataTupleCategoryDataset;
import com.ibm.nmon.gui.chart.data.DataTupleDataset;
import com.ibm.nmon.gui.chart.data.DataTupleXYDataset;
import com.ibm.nmon.interval.Interval;

/**
 * Helper class for writing CSV data to a Writer.
 */
public final class CSVWriter {
    private static final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
    private static final DecimalFormat FORMAT = new DecimalFormat("0.000");

    public static final void write(DataSet data, Interval interval, Writer writer) throws IOException {
        writer.write("Date,Time,");

        int n = 0;
        int i = 0;

        for (DataType type : data.getTypes()) {
            n = type.getFieldCount() - 1;
            i = 0;

            for (String field : type.getFields()) {
                writer.write(type.toString());
                writer.write(' ');
                writer.write(field);

                if (i++ < n) {
                    writer.write(',');
                }
            }
        }

        writer.write('\n');

        for (DataRecord record : data.getRecords(interval)) {
            writer.write(DATETIME.format(new java.util.Date(record.getTime())));
            writer.write(',');

            for (DataType type : data.getTypes()) {
                if (record.hasData(type)) {
                    n = type.getFieldCount() - 1;
                    i = 0;

                    for (String field : type.getFields()) {
                        writer.write(format(record.getData(type, field)));

                        if (i++ < n) {
                            writer.write(',');
                        }
                    }
                }
            }

            writer.write('\n');
        }
    }

    public static final void write(DataSet data, DataType type, Interval interval, Writer writer) throws IOException {
        write(data, type, type.getFields(), interval, writer);
    }

    public static final void write(DataSet data, DataType type, String field, Interval interval, Writer writer)
            throws IOException {
        write(data, type, java.util.Collections.singletonList(field), interval, writer);
    }

    public static final void write(DataSet data, DataType type, List<String> fields, Interval interval, Writer writer)
            throws IOException {

        writer.write("Date,Time,");

        for (int i = 0; i < fields.size() - 1; i++) {
            writer.write(fields.get(i));
            writer.write(',');
        }

        writer.write(fields.get(fields.size() - 1));
        writer.write('\n');

        for (DataRecord record : data.getRecords(interval)) {
            writer.write(DATETIME.format(new java.util.Date(record.getTime())));
            writer.write(',');

            if (record.hasData(type)) {
                for (int i = 0; i < fields.size() - 1; i++) {
                    writer.write(FORMAT.format(record.getData(type, fields.get(i))));
                    writer.write(',');
                }

                writer.write(FORMAT.format(record.getData(type, fields.get(fields.size() - 1))));
            }

            writer.write('\n');
        }
    }

    public static void write(DataTupleDataset data, Writer writer) throws IOException {
        if (data instanceof DataTupleXYDataset) {
            write((DataTupleXYDataset) data, writer);
        }
        else if (data instanceof DataTupleCategoryDataset) {
            write((DataTupleCategoryDataset) data, writer);
        }
    }

    public static void write(DataTupleXYDataset data, Writer writer) throws IOException {
        writer.write("Date,Time,");

        int seriesCount = data.getSeriesCount();

        for (int i = 0; i < seriesCount - 1; i++) {
            writer.write(data.getSeriesKey(i).toString());
            writer.write(',');
        }

        writer.write(data.getSeriesKey(seriesCount - 1).toString());
        writer.write('\n');

        for (int i = 0; i < data.getItemCount(); i++) {
            writer.write(DATETIME.format(data.getTimePeriod(i).getEnd()));
            writer.write(',');

            for (int j = 0; j < seriesCount - 1; j++) {
                Number n = data.getY(j, i);

                if (n != null) {
                    writer.write(format(n.doubleValue()));
                }

                writer.write(',');

            }

            writer.write(format(data.getY(seriesCount - 1, i).doubleValue()));
            writer.write('\n');
        }
    }

    public static void write(DataTupleCategoryDataset data, Writer writer) throws IOException {
        // output series names, leaving a blank column for item names
        writer.write(',');

        int columnCount = data.getColumnCount();
        for (int i = 0; i < columnCount - 1; i++) {
            writer.write(data.getColumnKey(i).toString());
            writer.write(',');
        }

        writer.write(data.getColumnKey(columnCount - 1).toString());
        writer.write('\n');

        for (int i = 0; i < data.getRowCount(); i++) {
            @SuppressWarnings("rawtypes")
            Comparable rowKey = data.getRowKey(i);

            writer.write(rowKey.toString());
            writer.write(',');

            for (int j = 0; j < columnCount - 1; j++) {
                Object o = data.getValue(rowKey, data.getColumnKey(j));

                if (o == null) {
                    writer.write(format(Double.NaN));
                }
                else {
                    writer.write(format(((Double) o).doubleValue()));
                }

                writer.write(',');
            }

            Object o = data.getValue(rowKey, data.getColumnKey(columnCount - 1));

            if (o == null) {
                writer.write(format(Double.NaN));
            }
            else {
                writer.write(format(((Double) o).doubleValue()));
            }

            writer.write('\n');
        }
    }

    private static String format(Double value) {
        if (Double.isNaN(value)) {
            return "";
        }
        else {
            return FORMAT.format(value);
        }
    }

    private CSVWriter() {}
}
