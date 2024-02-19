package com.ibm.nmon.gui.chart.data;

import java.util.List;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import com.ibm.nmon.data.DataTuple;

public final class DataTupleHistogramDataset extends HistogramDataset implements DataTupleDataset {
    private static final long serialVersionUID = -5932939796158641508L;

    private final List<DataTuple> tuples;

    private GraphData[] graphData;

    private final DatasetCallback callback = new DatasetCallback() {
        @Override
        public int getDataCount() {
            return DataTupleHistogramDataset.this.getSeriesCount();
        }

        @Override
        public int getItemCount(int dataIdx) {
            return DataTupleHistogramDataset.this.getItemCount(dataIdx);
        }

        @Override
        public double getValue(int dataIdx, int itemIdx) {
            return DataTupleHistogramDataset.this.getYValue(dataIdx, itemIdx);
        }
    };

    public DataTupleHistogramDataset() {
        super();

        tuples = new java.util.ArrayList<DataTuple>();
    }

    @Override
    public Number getY(int series, int item) {
        Number toReturn = super.getY(series, item);

        // default implementation uses a 0 to 1 scale; we want 0 to 100
        if (getType() == HistogramType.RELATIVE_FREQUENCY) {
            return toReturn.doubleValue() * 100;
        }
        else {
            return toReturn;
        }
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

    @Override
    public double getAverage(int row) {
        calculateGraphData();
        return graphData[row].average;
    }

    @Override
    public double getWeightedAverage(int row) {
        calculateGraphData();
        return graphData[row].weightedAverage;
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

    public DataTupleHistogramDataset merge(DataTupleHistogramDataset other) {
        DataTupleHistogramDataset toReturn = new DataTupleHistogramDataset();

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
            return tuples.equals(((DataTupleHistogramDataset) obj).tuples);
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
