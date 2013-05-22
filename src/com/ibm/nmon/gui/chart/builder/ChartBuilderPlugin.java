package com.ibm.nmon.gui.chart.builder;

import org.jfree.chart.JFreeChart;

public interface ChartBuilderPlugin {
    public void configureChart(JFreeChart chart);
}
