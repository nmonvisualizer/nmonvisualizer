package com.ibm.nmon.gui.chart.data;

import java.util.List;

import org.jfree.data.time.TimeTableXYDataset;

import com.ibm.nmon.data.DataTuple;

public final class DataTupleXYDataset extends TimeTableXYDataset implements DataTupleDataset {
    private final List<DataTuple> tuples;

    private final boolean stacked;
    private GraphData[] graphData;

    public DataTupleXYDataset(boolean stacked) {
        super();

        tuples = new java.util.ArrayList<DataTuple>();
        this.stacked = stacked;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void associateTuple(Comparable rowKey, Comparable columnKey, DataTuple tuple) {
        int idx = indexOf(rowKey);

        if (idx != -1) {
            tuples.add(idx, tuple);
            graphData = null;
        }
    }

    @Override
    public DataTuple getTuple(int row, int column) {
        return tuples.get(row);
    }

    public boolean isStacked() {
        return stacked;
    }

    @Override
    public double getAverage(int row) {
        calculateGraphData();
        return graphData[row].average;
    }

    @Override
    public double getMinimum(int row) {
        calculateGraphData();
        return graphData[row].minimum;
    }

    @Override
    public double getMaximum(int row) {
        calculateGraphData();
        return graphData[row].maximum;
    }

    @Override
    public double getMedian(int row) {
        calculateGraphData();
        return graphData[row].median;
    }

    @Override
    public double getStandardDeviation(int row) {
        calculateGraphData();
        return graphData[row].standardDeviation;
    }

    @Override
    public double getSum(int row) {
        calculateGraphData();
        return graphData[row].sum;
    }

    @Override
    public int getCount(int row) {
        calculateGraphData();
        return graphData[row].count;
    }

    public boolean containsTuple(DataTuple tuple) {
        return tuples.contains(tuple);
    }

    private void calculateGraphData() {
        if (graphData != null) {
            return;
        }

        graphData = new GraphData[tuples.size()];

        for (int i = 0; i < getSeriesCount(); i++) {
            GraphData data = new GraphData();
            graphData[i] = data;

            List<Double> allValues = new java.util.ArrayList<Double>(getItemCount(i));

            for (int j = 0; j < getItemCount(i); j++) {
                double value = getYValue(i, j);

                if (Double.isNaN(value)) {
                    continue;
                }

                data.sum += value;

                if (value > data.maximum) {
                    data.maximum = value;
                }

                if (value < data.minimum) {
                    data.minimum = value;
                }

                allValues.add(value);
            }

            if (allValues.size() > 0) {
                data.count = allValues.size();
                data.average = data.sum / data.count;

                java.util.Collections.sort(allValues);

                int idx = allValues.size() / 2;

                if ((data.count % 2) == 0) {
                    data.median = (allValues.get(idx) + allValues.get(idx - 1)) / 2;
                }
                else {
                    data.median = allValues.get(idx);
                }

                double sumSqDiffs = 0;

                for (double value : allValues) {
                    sumSqDiffs += Math.pow(value - data.average, 2);
                }

                data.standardDeviation = Math.sqrt(sumSqDiffs / data.count);
            }
            else {
                // file has data, but not for the given interval
                // set all values to NaN
                data.maximum = Double.NaN;
                data.minimum = Double.NaN;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        else if (obj.getClass() == this.getClass()) {
            return tuples.equals(((DataTupleXYDataset) obj).tuples);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return tuples.hashCode();
    }

    public static final DataTupleXYDataset merge(DataTupleXYDataset d1, DataTupleXYDataset d2) {
        DataTupleXYDataset toReturn = new DataTupleXYDataset(d1.stacked || d2.stacked);

        toReturn.tuples.addAll(d1.tuples);
        toReturn.tuples.addAll(d2.tuples);

        return toReturn;
    }
}
