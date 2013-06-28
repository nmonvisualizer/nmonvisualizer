package com.ibm.nmon.gui.chart.data;

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
}