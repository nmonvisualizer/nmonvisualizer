package com.ibm.nmon.gui.chart;

import org.slf4j.Logger;

import java.util.List;

import org.jfree.chart.JFreeChart;

import com.ibm.nmon.NMONVisualizerApp;

import com.ibm.nmon.interval.Interval;

import com.ibm.nmon.analysis.AnalysisRecord;
import com.ibm.nmon.chart.definition.*;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.chart.builder.*;

/**
 * Helper class for building {@link JFreeChart charts} from {@link BaseChartDefinition chart
 * definitions}.
 */
public class ChartFactory {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ChartFactory.class);

    private final NMONVisualizerApp app;

    private final LineChartBuilder lineChartBuilder;
    private final BarChartBuilder barChartBuilder;
    private final IntervalChartBuilder intervalChartBuilder;
    private final HistogramChartBuilder histogramChartBuilder;

    public ChartFactory(NMONVisualizerApp app) {
        this.app = app;

        lineChartBuilder = new LineChartBuilder();
        lineChartBuilder.addPlugin(new LineChartBuilderPlugin(app));

        barChartBuilder = new BarChartBuilder();
        intervalChartBuilder = new IntervalChartBuilder();
        histogramChartBuilder = new HistogramChartBuilder();
    }

    public void setGranularity(int granularity) {
        lineChartBuilder.setGranularity(granularity);
        barChartBuilder.setGranularity(granularity);
        intervalChartBuilder.setGranularity(granularity);
        histogramChartBuilder.setGranularity(granularity);
    }
    
    public void showLegends(boolean showLegends) {
        lineChartBuilder.showLegends(showLegends);
    }

    public void setInterval(Interval interval) {
        lineChartBuilder.setInterval(interval);
        barChartBuilder.setInterval(interval);
        intervalChartBuilder.setInterval(interval);
        histogramChartBuilder.setInterval(interval);
    }

    public void addPlugin(ChartBuilderPlugin plugin) {
        lineChartBuilder.addPlugin(plugin);
        barChartBuilder.addPlugin(plugin);
        intervalChartBuilder.addPlugin(plugin);
        histogramChartBuilder.addPlugin(plugin);
    }

    /**
     * Create a chart given a definition and some data.
     * 
     * @param definition the chart to create
     * @param dataSets the data to use for the chart
     * @return the chart
     * @see LineChartBuilder
     * @see BarChartBuilder
     * @see IntervalChartBuilder
     * @see HistogramChartBuilder
     */
    public JFreeChart createChart(BaseChartDefinition definition, Iterable<? extends DataSet> dataSets) {
        long startT = System.nanoTime();

        JFreeChart chart = null;

        if (definition.getClass().equals(LineChartDefinition.class)) {
            LineChartDefinition lineDefinition = (LineChartDefinition) definition;

            lineChartBuilder.initChart(lineDefinition);

            for (DataSet data : dataSets) {
                lineChartBuilder.addLine(data);
            }

            chart = lineChartBuilder.getChart();
        }
        else if (definition.getClass().equals(IntervalChartDefinition.class)) {
            IntervalChartDefinition lineDefinition = (IntervalChartDefinition) definition;

            intervalChartBuilder.initChart(lineDefinition);

            for (DataSet data : dataSets) {
                // TODO AnalysisRecord cache needed here?
                List<AnalysisRecord> analysis = new java.util.ArrayList<AnalysisRecord>();

                for (Interval i : app.getIntervalManager().getIntervals()) {
                    AnalysisRecord record = new AnalysisRecord(data);
                    record.setInterval(i);
                    record.setGranularity(intervalChartBuilder.getGranularity());

                    analysis.add(record);
                }

                intervalChartBuilder.addLine(lineDefinition, analysis);
            }

            chart = intervalChartBuilder.getChart();
        }
        else if (definition.getClass().equals(BarChartDefinition.class)) {
            BarChartDefinition barDefinition = (BarChartDefinition) definition;

            barChartBuilder.initChart(barDefinition);

            for (DataSet data : dataSets) {
                AnalysisRecord record = app.getAnalysis(data);

                // this check is really a hack for event interactions between the tree and the
                // ReportPanel when removing data with selected charts
                if (record != null) {
                    barChartBuilder.addBar(record);
                }
            }

            chart = barChartBuilder.getChart();
        }
        else if (definition.getClass().equals(HistogramChartDefinition.class)) {
            HistogramChartDefinition histogramDefinition = (HistogramChartDefinition) definition;

            histogramChartBuilder.initChart(histogramDefinition);

            for (DataSet data : dataSets) {
                AnalysisRecord record = app.getAnalysis(data);

                // this check is really a hack for event interactions between the tree and the
                // ReportPanel when removing data with selected charts
                if (record != null) {
                    histogramChartBuilder.addHistogram(record);
                }
            }

            chart = histogramChartBuilder.getChart();
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}: {} chart created in {}ms",
                    new Object[] { dataSets, definition.getShortName(), (System.nanoTime() - startT) / 1000000.0d });
        }

        return chart;
    }
}
