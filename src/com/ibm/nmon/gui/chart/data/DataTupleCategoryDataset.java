package com.ibm.nmon.gui.chart.data;

import java.util.Map;
import java.util.Set;

import org.jfree.data.category.DefaultCategoryDataset;

import com.ibm.nmon.data.DataTuple;

public final class DataTupleCategoryDataset extends DefaultCategoryDataset implements DataTupleDataset {
    private static final long serialVersionUID = -3658914756373575628L;

    private final Map<String, Map<String, DataTuple>> tuples;

    private final boolean intervals;
    private boolean categoriesHaveDifferentStats;

    private GraphData[] graphData;

    private DatasetCallback callback = new DatasetCallback() {
        @Override
        public int getDataCount() {
            return DataTupleCategoryDataset.this.getRowCount();
        }

        @Override
        public int getItemCount(int dataIdx) {
            return DataTupleCategoryDataset.this.getColumnCount();
        }

        @Override
        public double getValue(int dataIdx, int itemIdx) {
            return (Double) DataTupleCategoryDataset.this.getValue(dataIdx, itemIdx);
        }
    };

    public DataTupleCategoryDataset(boolean containsIntervals) {
        super();

        tuples = new java.util.HashMap<String, Map<String, DataTuple>>();
        intervals = containsIntervals;
        categoriesHaveDifferentStats = false;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void associateTuple(Comparable rowKey, Comparable columnKey, DataTuple tuple) {
        if ((rowKey == null) || (columnKey == null) || (tuple == null)) {
            return;
        }

        Map<String, DataTuple> columns = tuples.get(rowKey.toString());

        if (columns == null) {
            columns = new java.util.HashMap<String, DataTuple>(3);
            tuples.put(rowKey.toString(), columns);
        }

        columns.put(columnKey.toString(), tuple);

        graphData = null;
    }

    @Override
    public DataTuple getTuple(int row, int column) {
        if (intervals) {
            Map<String, DataTuple> columns = tuples.get(getRowKey(column).toString());

            if (columns == null) {
                return null;
            }
            else {
                return columns.get(getColumnKey(row).toString());
            }
        }
        else {
            Map<String, DataTuple> columns = tuples.get(getRowKey(row).toString());

            if (columns == null) {
                return null;
            }
            else {
                return columns.get(getColumnKey(column).toString());
            }
        }
    }

    @Override
    public Iterable<DataTuple> getAllTuples() {
        Set<DataTuple> allTuples = new java.util.HashSet<DataTuple>();

        for (String key : tuples.keySet()) {
            allTuples.addAll(tuples.get(key).values());
        }

        return java.util.Collections.unmodifiableSet(allTuples);
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
        return graphData[row].median;
    }

    @Override
    public double get99thPercentile(int row) {
        calculateGraphData();
        return graphData[row].percentile95;
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
        for (Map<String, DataTuple> columns : tuples.values()) {
            if (columns.containsValue(tuple)) {
                return true;
            }
        }

        return false;
    }

    public DataTupleCategoryDataset merge(DataTupleCategoryDataset other) {
        DataTupleCategoryDataset toReturn = new DataTupleCategoryDataset(other.containsIntervals());

        toReturn.categoriesHaveDifferentStats = this.categoriesHaveDifferentStats | other.categoriesHaveDifferentStats;

        toReturn.tuples.putAll(this.tuples);
        toReturn.tuples.putAll(other.tuples);

        return toReturn;
    }

    private void calculateGraphData() {
        if (graphData != null) {
            return;
        }
        else {
            // bar charts do not have any graph data
            if (!intervals) {
                int size = getRowCount() * getColumnCount();
                graphData = new GraphData[size];

                for (int i = 0; i < size; i++) {
                    graphData[i] = new GraphData();
                    graphData[i].minimum = Double.NaN;
                    graphData[i].maximum = Double.NaN;
                }
            }
            else {
                graphData = GraphData.calculate(callback);
            }
        }
    }

    public boolean containsIntervals() {
        return intervals;
    }

    public boolean categoriesHaveDifferentStats() {
        return categoriesHaveDifferentStats;
    }

    public void setCategoriesHaveDifferentStats(boolean categoriesHaveDifferentStats) {
        this.categoriesHaveDifferentStats = categoriesHaveDifferentStats;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        else if (obj.getClass() == this.getClass()) {
            return tuples.equals(((DataTupleCategoryDataset) obj).tuples);
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
