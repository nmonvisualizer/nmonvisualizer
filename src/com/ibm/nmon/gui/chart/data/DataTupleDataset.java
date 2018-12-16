package com.ibm.nmon.gui.chart.data;

import com.ibm.nmon.data.DataTuple;

/**
 * Interface for JFreeChart Datasets that can associate {@link DataTuple DataTuples} with a given
 * data point.
 */
public interface DataTupleDataset {
    @SuppressWarnings("rawtypes")
    public void associateTuple(Comparable rowKey, Comparable columnKey, DataTuple tuple);

    public DataTuple getTuple(int row, int column);

    public Iterable<DataTuple> getAllTuples();

    public double getWeightedAverage(int row);
    
    public double getAverage(int row);

    public double getMinimum(int row);

    public double getMaximum(int row);

    public double getMedian(int row);

    public double get95thPercentile(int row);

    public double get99thPercentile(int row);

    public double getStandardDeviation(int row);

    public double getSum(int row);

    public int getCount(int row);

    public boolean containsTuple(DataTuple tuple);
}
