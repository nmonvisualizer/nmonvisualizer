package com.ibm.nmon.gui.chart.data;

import java.util.List;

import com.ibm.nmon.analysis.AnalysisRecord;

final class GraphData {
    int count = 0;
    double sum = 0;

    double average = Double.NaN;

    double median = Double.NaN;
    double percentile95 = Double.NaN;
    double percentile99 = Double.NaN;

    double minimum = Double.MAX_VALUE;
    double maximum = Double.MIN_VALUE;

    double standardDeviation = Double.NaN;

    static GraphData[] calculate(DatasetCallback callback) {
        int dataCount = callback.getDataCount();
        GraphData[] graphData = new GraphData[dataCount];

        for (int i = 0; i < dataCount; i++) {
            GraphData data = new GraphData();
            graphData[i] = data;

            int itemCount = callback.getItemCount(i);

            List<Double> allValues = new java.util.ArrayList<Double>(itemCount);

            for (int j = 0; j < itemCount; j++) {
                double value = callback.getValue(i, j);

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
                // file has data, but not for the given interval
                // set all values to NaN
                data.maximum = Double.NaN;
                data.minimum = Double.NaN;
            }
        }

        return graphData;
    }
}