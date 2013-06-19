package com.ibm.nmon.gui.chart;

import java.util.List;

import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;

import com.ibm.nmon.NMONVisualizerApp;
import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.definition.DataDefinition;

import com.ibm.nmon.analysis.AnalysisRecord;
import com.ibm.nmon.chart.definition.*;
import com.ibm.nmon.gui.chart.builder.*;

import com.ibm.nmon.interval.Interval;

/**
 * Helper class for building {@link JFreeChart charts} from {@link BaseChartDefinition chart
 * definitions}.
 */
public final class ChartFactory {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ChartFactory.class);

    private final NMONVisualizerApp app;

    private final LineChartBuilder lineChartBuilder;
    private final BarChartBuilder barChartBuilder;
    private final IntervalChartBuilder intervalChartBuilder;

    public ChartFactory(NMONVisualizerApp app) {
        this.app = app;

        lineChartBuilder = new LineChartBuilder();
        lineChartBuilder.addPlugin(new LineChartBuilderPlugin(app));

        barChartBuilder = new BarChartBuilder();
        intervalChartBuilder = new IntervalChartBuilder();
    }

    public void setGranularity(int granularity) {
        lineChartBuilder.setGranularity(granularity);
        barChartBuilder.setGranularity(granularity);
        intervalChartBuilder.setGranularity(granularity);
    }

    public void setInterval(Interval interval) {
        lineChartBuilder.setInterval(interval);
        barChartBuilder.setInterval(interval);
        intervalChartBuilder.setInterval(interval);
    }

    public void addPlugin(ChartBuilderPlugin plugin) {
        lineChartBuilder.addPlugin(plugin);
        barChartBuilder.addPlugin(plugin);
        intervalChartBuilder.addPlugin(plugin);
    }

    /**
     * Filter a set of chart definitions based on a given data set. Charts that are not applicable
     * to a host in the data set will not be included in the returned list.
     * 
     * @param chartDefinitions the set of charts to filter
     * @param dataSets the DataSets to match
     * @return the filtered set of charts
     * @see DataDefinition#matchesHost(DataSet)
     */
    public List<BaseChartDefinition> getChartsForData(Iterable<BaseChartDefinition> chartDefinitions,
            Iterable<? extends DataSet> dataSets) {
        List<BaseChartDefinition> toReturn = new java.util.ArrayList<BaseChartDefinition>();

        // the charts actually used depend on the host
        // if any DataSet matches a defined host, show the report
        for (BaseChartDefinition chartDefinition : chartDefinitions) {
            dataset: for (DataSet data : dataSets) {
                if (chartDefinition instanceof LineChartDefinition) {
                    // also handles IntervalLineCharts
                    for (DataDefinition definition : ((LineChartDefinition) chartDefinition).getLines()) {
                        if (definition.matchesHost(data)) {
                            toReturn.add(chartDefinition);
                            break dataset;
                        }
                    }
                }
                else {
                    for (DataDefinition definition : ((BarChartDefinition) chartDefinition).getCategories()) {
                        if (definition.matchesHost(data)) {
                            toReturn.add(chartDefinition);
                            break dataset;
                        }
                    }
                }
            }
        }

        return toReturn;
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
     */
    public JFreeChart createChart(BaseChartDefinition definition, Iterable<? extends DataSet> dataSets) {
        long startT = System.nanoTime();

        JFreeChart chart = null;
        int dataSetCount = 0;

        if (definition.getClass().equals(LineChartDefinition.class)) {
            LineChartDefinition lineDefinition = (LineChartDefinition) definition;

            lineChartBuilder.initChart(lineDefinition);

            for (DataSet data : dataSets) {
                lineChartBuilder.addLine(lineDefinition, data);
                ++dataSetCount;
            }

            chart = lineChartBuilder.getChart();
        }
        else if (definition.getClass().equals(IntervalChartDefinition.class)) {
            IntervalChartDefinition lineDefinition = (IntervalChartDefinition) definition;

            intervalChartBuilder.initChart(lineDefinition);

            int intervalCount = app.getIntervalManager().getIntervalCount();

            for (DataSet data : dataSets) {
                // TODO AnalysisRecord cache needed here?
                List<AnalysisRecord> analysis = new java.util.ArrayList<AnalysisRecord>(intervalCount);

                for (Interval i : app.getIntervalManager().getIntervals()) {
                    AnalysisRecord record = new AnalysisRecord(data);
                    record.setInterval(i);
                    record.setGranularity(intervalChartBuilder.getGranularity());

                    analysis.add(record);
                }

                intervalChartBuilder.addLine(lineDefinition, analysis);
                ++dataSetCount;
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
                    barChartBuilder.addBar(barDefinition, record);
                }

                ++dataSetCount;
            }

            chart = barChartBuilder.getChart();
        }

        if (dataSetCount == 1) {
            chart.setTitle(chart.getTitle().getText() + '\n' + dataSets.iterator().next().getHostname());
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}: {} chart created in {}ms",
                    new Object[] { dataSets, definition.getShortName(), (System.nanoTime() - startT) / 1000000.0d });
        }

        return chart;
    }
}
