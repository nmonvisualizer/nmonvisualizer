package com.ibm.nmon.gui.chart.data;

import java.util.List;

import org.jfree.data.time.TimeTableXYDataset;

import com.ibm.nmon.data.DataTuple;

public final class DataTupleXYDataset extends TimeTableXYDataset implements DataTupleDataset {
    private static final long serialVersionUID = 9065578614822952026L;

    private final List<DataTuple> tuples;

    private final boolean stacked;
    private GraphData[] graphData;

    private DatasetCallback callback = new DatasetCallback() {
        @Override
        public int getDataCount() {
            return DataTupleXYDataset.this.getSeriesCount();
        }

        @Override
        public int getItemCount(int dataIdx) {
            return DataTupleXYDataset.this.getItemCount(dataIdx);
        }

        @Override
        public double getValue(int dataIdx, int itemIdx) {
            return DataTupleXYDataset.this.getYValue(dataIdx, itemIdx);
        }
    };

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

    @Override
    public Iterable<DataTuple> getAllTuples() {
        return java.util.Collections.unmodifiableList(tuples);
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
    public double get95thPercentile(int row) {
        calculateGraphData();
        return graphData[row].percentile95;
    }

    @Override
    public double get99thPercentile(int row) {
        calculateGraphData();
        return graphData[row].percentile99;
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

    public final DataTupleXYDataset merge(DataTupleXYDataset other) {
        DataTupleXYDataset toReturn = new DataTupleXYDataset(this.stacked || other.stacked);

        toReturn.tuples.addAll(this.tuples);
        toReturn.tuples.addAll(other.tuples);

        return toReturn;
    }

    private void calculateGraphData() {
        if (graphData != null) {
            return;
        }
        else {
            graphData = GraphData.calculate(callback);
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
}
