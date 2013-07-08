package com.ibm.nmon.report;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.File;

import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CategoryPlot;

import com.ibm.nmon.NMONVisualizerApp;

import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.interval.IntervalListener;

import com.ibm.nmon.parser.ChartDefinitionParser;
import com.ibm.nmon.chart.definition.*;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

import com.ibm.nmon.data.definition.DataDefinition;
import com.ibm.nmon.data.definition.DefaultDataDefinition;
import com.ibm.nmon.data.definition.ExactDataDefinition;
import com.ibm.nmon.data.definition.NamingMode;
import com.ibm.nmon.data.matcher.ExactFieldMatcher;
import com.ibm.nmon.data.matcher.ExactTypeMatcher;

import com.ibm.nmon.gui.chart.ChartFactory;

import com.ibm.nmon.util.GranularityHelper;

/**
 * <p>
 * Factory for creating a set of charts and saving them to PNGs.
 * </p>
 * 
 * <p>
 * This class caches {@link BaseChartDefinition chart definitions} with a key. This key can then be
 * used to create {@link #createChartsAcrossDataSets summary charts} or to create
 * {@link #createChartsForEachDataSet charts for each data set}.
 * </p>
 * 
 * @see ChartFactory
 * @see ChartDefinitionParser
 */
public class ReportFactory implements IntervalListener {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ReportFactory.class);

    public static final String DEFAULT_SUMMARY_CHARTS_KEY = "summary";
    public static final String DEFAULT_DATASET_CHARTS_KEY = "dataset";

    private final NMONVisualizerApp app;

    private final ChartDefinitionParser parser = new ChartDefinitionParser();
    private final GranularityHelper granularityHelper;
    private final ChartFactory chartFactory;

    private Map<String, List<BaseChartDefinition>> chartDefinitionsCache = new java.util.HashMap<String, List<BaseChartDefinition>>();

    public ReportFactory(NMONVisualizerApp app) {
        this.app = app;

        granularityHelper = new GranularityHelper(app);
        chartFactory = new ChartFactory(app);

        granularityHelper.setAutomatic(true);
        chartFactory.setGranularity(granularityHelper.getGranularity());

        try {
            chartDefinitionsCache.put(DEFAULT_SUMMARY_CHARTS_KEY, parser.parseCharts(ReportFactory.class
                    .getResourceAsStream("/com/ibm/nmon/report/summary_single_interval.xml")));
            chartDefinitionsCache.put(DEFAULT_DATASET_CHARTS_KEY, parser.parseCharts(ReportFactory.class
                    .getResourceAsStream("/com/ibm/nmon/report/dataset_report.xml")));
        }
        catch (IOException e) {
            LOGGER.error("cannot parse default report definition xmls", e);
        }

        app.getIntervalManager().addListener(this);
    }

    public void addChartDefinition(String key, String file, ReportFactoryCallback callback) {
        try {
            chartDefinitionsCache.put(key, parser.parseCharts(file));
            LOGGER.debug("loaded chart definitions from '{}'", file);
            callback.onChartDefinitionAdded(key, file);
        }
        catch (IOException ioe) {
            LOGGER.error("cannot parse report definition xml from '{}'", file);
            callback.onChartDefinitionFailure(key, file, ioe);
        }
    }

    /**
     * Creates a single set of chart for all the currently parsed DataSets.
     */
    public void createChartsAcrossDataSets(String chartDefinitionKey, File chartDirectory,
            ReportFactoryCallback callback) {
        List<BaseChartDefinition> chartDefinitions = chartDefinitionsCache.get(chartDefinitionKey);

        if (chartDefinitions != null) {
            chartDirectory.mkdirs();

            LOGGER.debug("creating charts for '{}'", chartDefinitionKey);

            List<DataSet> list = new java.util.ArrayList<DataSet>();

            for (DataSet data : app.getDataSets()) {
                list.add(data);
            }

            callback.beforeCreateCharts(chartDefinitionKey, list, chartDirectory.getAbsolutePath());
            int chartsCreated = saveCharts(chartDefinitions, app.getDataSets(), chartDirectory, callback);
            callback.afterCreateCharts(chartDefinitionKey, list, chartDirectory.getAbsolutePath());

            LOGGER.debug("created {} charts for '{}'", chartsCreated, chartDefinitionKey);

            // remove the directory if no images were created
            if (chartsCreated == 0) {
                LOGGER.debug("removing unused directory '{}'", chartDirectory);
                chartDirectory.delete();
            }
        }
    }

    /**
     * Creates a set of charts for <em>each</em> of the parsed DataSets. Each DataSet will create a
     * sub-directory off of the given directory, named by the DataSet's hostname.
     */
    public void createChartsForEachDataSet(String chartDefinitionKey, File chartDirectory,
            ReportFactoryCallback callback) {
        List<BaseChartDefinition> datasetChartDefinitions = chartDefinitionsCache.get(chartDefinitionKey);

        if (datasetChartDefinitions != null) {
            chartDirectory.mkdirs();

            for (DataSet data : app.getDataSets()) {
                LOGGER.debug("creating charts for '{}' for dataset {}", chartDefinitionKey, data.getHostname());

                // create a subdirectory for each dataset
                File datasetChartsDir = new File(chartDirectory, data.getHostname());
                datasetChartsDir.mkdir();

                List<DataSet> list = java.util.Collections.singletonList(data);
                callback.beforeCreateCharts(chartDefinitionKey, list, chartDirectory.getAbsolutePath());
                int chartsCreated = saveCharts(datasetChartDefinitions, list, datasetChartsDir, callback);
                callback.afterCreateCharts(chartDefinitionKey, list, chartDirectory.getAbsolutePath());

                LOGGER.debug("created {} charts for '{}'", chartsCreated, chartDefinitionKey);

                // remove the directory if no images were created
                if (chartsCreated == 0) {
                    LOGGER.debug("removing unused directory '{}'", chartDirectory);
                    datasetChartsDir.delete();
                }
            }
        }
    }

    /**
     * <p>
     * Creates multiple charts per field. Rather than creating a single chart with a line/bar for
     * each field, this function creates a chart for <em>each</em> field that matches the given
     * definition.
     * </p>
     * <p>
     * This function is applied across all currently parsed DataSets. It also uses all charts in the
     * given report. So it is possible for this function to create a large number of charts,
     * especially if multiple DataTypes and/or fields are matched.
     * </p>
     */
    public void multiplexChartsAcrossFields(String chartDefinitionKey, File chartDirectory,
            ReportFactoryCallback callback) {
        List<BaseChartDefinition> datasetChartDefinitions = chartDefinitionsCache.get(chartDefinitionKey);

        if (datasetChartDefinitions == null) {
            return;
        }

        chartDirectory.mkdirs();

        for (DataSet data : app.getDataSets()) {
            LOGGER.debug("multiplexing charts for '{}' for dataset {} across fields", chartDefinitionKey,
                    data.getHostname());

            // create a subdirectory for each dataset
            File datasetChartsDir = new File(chartDirectory, data.getHostname());
            datasetChartsDir.mkdir();

            List<BaseChartDefinition> multiplexedChartDefinitions = new java.util.ArrayList<BaseChartDefinition>(
                    3 * datasetChartDefinitions.size());

            for (BaseChartDefinition chartDefinition : datasetChartDefinitions) {
                for (DataDefinition dataDefinition : chartDefinition.getData()) {
                    if (dataDefinition.matchesHost(data)) {
                        for (DataType type : dataDefinition.getMatchingTypes(data)) {
                            for (String field : dataDefinition.getMatchingFields(type)) {
                                BaseChartDefinition newChartDefinition = copyChart(chartDefinition);

                                // short name used as filename and/or tabname, so make sure it
                                // is unique
                                newChartDefinition.setShortName(chartDefinition.getShortName() + "_"
                                        + dataDefinition.renameField(field));

                                newChartDefinition.setTitle(chartDefinition.getTitle());
                                newChartDefinition.setSubtitleNamingMode(NamingMode.FIELD);

                                DataDefinition newData = null;

                                if (dataDefinition instanceof DefaultDataDefinition) {
                                    DefaultDataDefinition old = (DefaultDataDefinition) dataDefinition;

                                    newData = old.withNewFields(new ExactFieldMatcher(field));
                                }
                                else {
                                    newData = new ExactDataDefinition(data, type,
                                            java.util.Collections.singletonList(field), dataDefinition.getStatistic(),
                                            dataDefinition.usesSecondaryYAxis());
                                }

                                newChartDefinition.addData(newData);

                                multiplexedChartDefinitions.add(newChartDefinition);
                            }
                        }
                    }
                }
            }

            int chartsCreated = 0;

            if (!multiplexedChartDefinitions.isEmpty()) {
                List<DataSet> list = java.util.Collections.singletonList(data);
                String newKey = chartDefinitionKey + " (" + data.getHostname() + ')';

                callback.beforeCreateCharts(newKey, list, chartDirectory.getAbsolutePath());
                chartsCreated = saveCharts(multiplexedChartDefinitions, list, datasetChartsDir, callback);
                callback.afterCreateCharts(newKey, list, chartDirectory.getAbsolutePath());
            }

            LOGGER.debug("multiplexed {} charts across fields for '{}'", chartsCreated, chartDefinitionKey);

            // remove the directory if no images were created
            if (chartsCreated == 0) {
                LOGGER.debug("removing unused directory '{}'", chartDirectory);
                datasetChartsDir.delete();
            }
        }
    }

    /**
     * <p>
     * Creates multiple charts per type. Rather than creating a single chart with a line/bar for
     * each type, this function creates a chart for <em>each</em> type that matches the given
     * definition.
     * </p>
     * <p>
     * This function is applied across all currently parsed DataSets. It also uses all charts in the
     * given report. So it is possible for this function to create a large number of charts,
     * especially if multiple DataTypes are matched.
     * </p>
     */
    public void multiplexChartsAcrossTypes(String chartDefinitionKey, File chartDirectory,
            ReportFactoryCallback callback) {
        List<BaseChartDefinition> datasetChartDefinitions = chartDefinitionsCache.get(chartDefinitionKey);

        if (datasetChartDefinitions == null) {
            return;
        }

        chartDirectory.mkdirs();

        for (DataSet data : app.getDataSets()) {
            LOGGER.debug("multiplexing charts for '{}' for dataset {} across fields", chartDefinitionKey,
                    data.getHostname());

            // create a subdirectory for each dataset
            File datasetChartsDir = new File(chartDirectory, data.getHostname());
            datasetChartsDir.mkdir();

            List<BaseChartDefinition> multiplexedChartDefinitions = new java.util.ArrayList<BaseChartDefinition>(
                    3 * datasetChartDefinitions.size());

            for (BaseChartDefinition chartDefinition : datasetChartDefinitions) {
                for (DataDefinition dataDefinition : chartDefinition.getData()) {
                    if (dataDefinition.matchesHost(data)) {
                        for (DataType type : dataDefinition.getMatchingTypes(data)) {
                            BaseChartDefinition newChartDefinition = copyChart(chartDefinition);

                            // short name used as filename and/or tabname, so make sure it
                            // is unique
                            newChartDefinition.setShortName(chartDefinition.getShortName() + "_"
                                    + dataDefinition.renameType(type));

                            newChartDefinition.setTitle(chartDefinition.getTitle());
                            newChartDefinition.setSubtitleNamingMode(NamingMode.TYPE);

                            DataDefinition newData = null;

                            if (dataDefinition instanceof DefaultDataDefinition) {
                                DefaultDataDefinition old = (DefaultDataDefinition) dataDefinition;

                                newData = old.withNewTypes(new ExactTypeMatcher(type.toString()));
                            }
                            else {
                                newData = new ExactDataDefinition(data, type, dataDefinition.getMatchingFields(type),
                                        dataDefinition.getStatistic(), dataDefinition.usesSecondaryYAxis());
                            }

                            newChartDefinition.addData(newData);

                            multiplexedChartDefinitions.add(newChartDefinition);
                        }
                    }
                }
            }

            int chartsCreated = 0;

            if (!multiplexedChartDefinitions.isEmpty()) {
                List<DataSet> list = java.util.Collections.singletonList(data);
                String newKey = chartDefinitionKey + " (" + data.getHostname() + ')';

                callback.beforeCreateCharts(newKey, list, chartDirectory.getAbsolutePath());
                chartsCreated = saveCharts(multiplexedChartDefinitions, list, datasetChartsDir, callback);
                callback.afterCreateCharts(newKey, list, chartDirectory.getAbsolutePath());
            }

            LOGGER.debug("multiplexed {} charts across fields for '{}'", chartsCreated, chartDefinitionKey);

            // remove the directory if no images were created
            if (chartsCreated == 0) {
                LOGGER.debug("removing unused directory '{}'", chartDirectory);
                datasetChartsDir.delete();
            }
        }
    }

    private int saveCharts(List<BaseChartDefinition> chartDefinitions, Iterable<? extends DataSet> data,
            File saveDirectory, ReportFactoryCallback callback) {

        List<BaseChartDefinition> chartsToCreate = chartFactory.getChartsForData(chartDefinitions, data);
        int chartsCreated = 0;

        for (BaseChartDefinition definition : chartsToCreate) {
            JFreeChart chart = chartFactory.createChart(definition, data);

            if (chartHasData(chart)) {
                String filename = definition.getShortName().replace('\n', ' ') + ".png";
                File chartFile = new File(saveDirectory, filename);

                try {
                    ChartUtilities.saveChartAsPNG(chartFile, chart, 1920 / 2, 1080 / 2);
                }
                catch (IOException ioe) {
                    LOGGER.warn("cannot create chart '{}'", chartFile.getName());
                    continue;
                }

                callback.onCreateChart(definition, chartFile.getAbsolutePath());
                ++chartsCreated;
            }
        }

        return chartsCreated;
    }

    private boolean chartHasData(JFreeChart chart) {
        boolean hasData = false;

        // determine if there will really be any data to display
        // do not output a chart if there is no data
        Plot plot = chart.getPlot();

        if (plot instanceof CategoryPlot) {
            CategoryPlot cPlot = (CategoryPlot) plot;

            for (int i = 0; i < cPlot.getDatasetCount(); i++) {
                if (cPlot.getDataset(i).getRowCount() > 0) {
                    hasData = true;
                    break;
                }
            }
        }
        else if (plot instanceof XYPlot) {
            XYPlot xyPlot = (XYPlot) plot;

            for (int i = 0; i < xyPlot.getDatasetCount(); i++) {
                if (xyPlot.getDataset(i).getSeriesCount() > 0) {
                    hasData = true;
                    break;
                }
            }
        }
        else {
            LOGGER.warn("unknown plot type {} for chart {}", plot.getClass(), chart.getTitle());
        }

        return hasData;
    }

    private BaseChartDefinition copyChart(BaseChartDefinition copy) {
        if (copy.getClass().equals(LineChartDefinition.class)) {
            return new LineChartDefinition((LineChartDefinition) copy, false);
        }
        else if (copy.getClass().equals(BarChartDefinition.class)) {
            return new BarChartDefinition((BarChartDefinition) copy, false);
        }
        else if (copy.getClass().equals(IntervalChartDefinition.class)) {
            return new IntervalChartDefinition((IntervalChartDefinition) copy, false);
        }
        else if (copy.getClass().equals(HistogramChartDefinition.class)) {
            return new HistogramChartDefinition((HistogramChartDefinition) copy, false);
        }
        else {
            return null;
        }
    }

    @Override
    public void intervalAdded(Interval interval) {}

    @Override
    public void intervalRemoved(Interval interval) {}

    @Override
    public void intervalsCleared() {}

    @Override
    public void currentIntervalChanged(Interval interval) {
        chartFactory.setInterval(interval);

        granularityHelper.recalculate();
        chartFactory.setGranularity(granularityHelper.getGranularity());
    }

    @Override
    public void intervalRenamed(Interval interval) {}

    /**
     * Callback class for visibility into the parsing of chart definition parsing and chart creation
     * of the factory. Clients can override this class to output status on the current item being
     * processed by the ReportFactory.
     */
    public static class ReportFactoryCallback {
        /** Called when a chart definition is successfully parsed **/
        public void onChartDefinitionAdded(String chartDefinitionKey, String definitionPath) {}

        /** Called when there is an exception parsing a chart definition **/
        public void onChartDefinitionFailure(String chartDefinitionKey, String definitionPath, IOException ioe) {}

        /** Called at the beginning of chart creation **/
        public void beforeCreateCharts(String chartDefinitionKey, List<DataSet> data, String savePath) {}

        /** Called after each chart in a chart definition is saved to the file system **/
        public void onCreateChart(BaseChartDefinition definition, String savePath) {}

        /** Called after all charts are created **/
        public void afterCreateCharts(String chartDefinitionKey, List<DataSet> data, String savePath) {}
    }
}
