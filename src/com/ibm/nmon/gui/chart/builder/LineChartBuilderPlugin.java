package com.ibm.nmon.gui.chart.builder;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;

import com.ibm.nmon.NMONVisualizerApp;
import com.ibm.nmon.gui.chart.TimeAndValueTooltipGenerator;
import com.ibm.nmon.interval.Interval;

/**
 * Plugin responsible for setting up the x-axis on a line chart.
 * <p/>
 * <p>
 * This plugin <em>must</em> be attached to any {@link LineChartBuilder} for the chart to display
 * correctly. This is necessary because the chart builder does not have a reference to the
 * {@link NMONVisualizerApp} which is needed to correctly format the x-axis based on the current
 * times for the data sets that have been parsed.
 * </p>
 */
public final class LineChartBuilderPlugin implements ChartBuilderPlugin {
    private final NMONVisualizerApp app;

    public LineChartBuilderPlugin(NMONVisualizerApp app) {
        this.app = app;
    }

    @Override
    public void configureChart(JFreeChart chart) {
        // this setup is required before any data is in the chart
        // or auto ranging on the y-axis breaks
        if (app.getBooleanProperty("chartRelativeTime")) {
            LineChartBuilder.setRelativeAxis(chart, app.getMinSystemTime());
        }
        else {
            LineChartBuilder.setAbsoluteAxis(chart);
        }

        DateAxis axis = (DateAxis) chart.getXYPlot().getDomainAxis();
        Interval current = app.getIntervalManager().getCurrentInterval();

        axis.setTimeZone(app.getDisplayTimeZone());

        if (chart.getXYPlot().getRenderer().getBaseToolTipGenerator().getClass() == TimeAndValueTooltipGenerator.class) {
            ((TimeAndValueTooltipGenerator) chart.getXYPlot().getRenderer().getBaseToolTipGenerator()).setTimeZone(app
                    .getDisplayTimeZone());
        }

        if (Interval.DEFAULT.equals(current)) {
            if (app.getMinSystemTime() == 0) {
                // implies no current datasets, so keep the axis range sane
                // fixes near infinite loop displaying chart with no data
                long now = System.currentTimeMillis();
                axis.setRange(now - (86400000 / 2), now + ((86400000 - 1000) / 2));
            }
            else {
                long min = app.getMinSystemTime();
                long max = app.getMaxSystemTime();

                // exception thrown if min and max are the same
                if (min == max) {
                    min -= 1000;
                }

                axis.setRange(min, max);
            }
        }
        else {
            axis.setRange(current.getStart(), current.getEnd());
        }
    }
}
