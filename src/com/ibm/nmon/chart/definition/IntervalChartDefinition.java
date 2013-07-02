package com.ibm.nmon.chart.definition;

/**
 * A specialization of LineChart used to denote charts that will display summary data for a number
 * of intervals.
 */
public final class IntervalChartDefinition extends LineChartDefinition {
    public IntervalChartDefinition(String shortName, String title) {
        // stacked is always false for IntervalCharts
        super(shortName, title, false);
    }

    public IntervalChartDefinition(IntervalChartDefinition copy, boolean copyData) {
        super(copy, copyData);
    }
}
