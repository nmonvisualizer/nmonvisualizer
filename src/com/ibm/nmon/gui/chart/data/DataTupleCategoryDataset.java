package com.ibm.nmon.gui.chart.data;

import java.util.List;
import java.util.Map;

import org.jfree.data.category.DefaultCategoryDataset;

import com.ibm.nmon.analysis.AnalysisRecord;
import com.ibm.nmon.data.DataTuple;

public final class DataTupleCategoryDataset extends DefaultCategoryDataset implements DataTupleDataset {
    private final Map<String, Map<String, DataTuple>> tuples;

    private final boolean intervals;
    private boolean categoriesHaveDifferentStats;

    private GraphData[] graphData;

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

    private void calculateGraphData() {
        if (graphData != null) {
            return;
        }

        // bar charts do not have any graph data
        if (!intervals) {
            int size = getRowCount() * getColumnCount();
            graphData = new GraphData[size];

            for (int i = 0; i < size; i++) {
                graphData[i] = new GraphData();
                graphData[i].minimum = Double.NaN;
                graphData[i].maximum = Double.NaN;
            }

            return;
        }

        graphData = new GraphData[getRowCount()];

        for (int i = 0; i < getRowCount(); i++) {
            GraphData data = new GraphData();
            graphData[i] = data;

            List<Double> allValues = new java.util.ArrayList<Double>(getColumnCount());

            for (int j = 0; j < getColumnCount(); j++) {
                double value = 0;

                if (intervals) {
                    Object o = getValue(i, j);

                    if (o == null) {
                        value = Double.NaN;
                    }
                    else {
                        value = (Double) o;
                    }
                }
                else {
                    value = (Double) getValue(j, i);
                }

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

                data.median = AnalysisRecord.calculatePercentile(.5, allValues);
                data.percentile95 = AnalysisRecord.calculatePercentile(.95, allValues);
                data.percentile99 = AnalysisRecord.calculatePercentile(.99, allValues);

                double sumSqDiffs = 0;

                for (double value : allValues) {
                    sumSqDiffs += Math.pow(value - data.average, 2);
                }

                data.standardDeviation = Math.sqrt(sumSqDiffs / data.count);
            }
            else {
                // set all values to NaN
                data.maximum = Double.NaN;
                data.minimum = Double.NaN;
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
