package com.ibm.nmon.report;

import java.io.IOException;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.ibm.nmon.chart.definition.BarChartDefinition;
import com.ibm.nmon.chart.definition.ChartDefinitionParser;
import com.ibm.nmon.chart.definition.BaseChartDefinition;
import com.ibm.nmon.chart.definition.HistogramChartDefinition;
import com.ibm.nmon.chart.definition.IntervalChartDefinition;
import com.ibm.nmon.chart.definition.LineChartDefinition;
import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.definition.DataDefinition;
import com.ibm.nmon.data.definition.DefaultDataDefinition;
import com.ibm.nmon.data.definition.ExactDataDefinition;
import com.ibm.nmon.data.definition.NamingMode;
import com.ibm.nmon.data.matcher.ExactFieldMatcher;
import com.ibm.nmon.data.matcher.ExactTypeMatcher;
import com.ibm.nmon.gui.chart.ChartFactory;

/**
 * A simple cache for storing 'reports', a list of parsed chart definitions. Reports are stored and
 * retrieved using a key, which can be any String. Cached reports can be filtered for a list of data
 * sets to avoid creating charts that are not needed for the application's current set of data.
 * 
 * @see BaseChartDefinition
 * @see ChartDefinitionParser
 */
public final class ReportCache {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ReportCache.class);

    public static final String DEFAULT_SUMMARY_CHARTS_KEY = "summary";
    public static final String DEFAULT_INTERVAL_CHARTS_KEY = "interval";
    public static final String DEFAULT_DATASET_CHARTS_KEY = "dataset";

    private final ChartDefinitionParser parser = new ChartDefinitionParser();

    private Map<String, List<BaseChartDefinition>> reports = new java.util.HashMap<String, List<BaseChartDefinition>>();

    public ReportCache() {
        try {
            reports.put(DEFAULT_SUMMARY_CHARTS_KEY, parser.parseCharts(ChartFactory.class
                    .getResourceAsStream("/com/ibm/nmon/report/summary_single_interval.xml")));
            reports.put(DEFAULT_INTERVAL_CHARTS_KEY, parser.parseCharts(ChartFactory.class
                    .getResourceAsStream("/com/ibm/nmon/report/summary_all_intervals.xml")));
            reports.put(DEFAULT_DATASET_CHARTS_KEY, parser.parseCharts(ChartFactory.class
                    .getResourceAsStream("/com/ibm/nmon/report/dataset_report.xml")));
        }
        catch (IOException e) {
            LOGGER.error("cannot parse default report definition xmls", e);
        }
    }

    /**
     * Parse the given chart definition XML file and store the report with the given key.
     * 
     * @param file a valid XML file for processing by {@link ChartDefinitionParser}
     */
    public void addChartDefinition(String key, String file) throws IOException {
        if (DEFAULT_SUMMARY_CHARTS_KEY.equals(key) || DEFAULT_INTERVAL_CHARTS_KEY.equals(key)
                || DEFAULT_DATASET_CHARTS_KEY.equals(key)) {
            throw new IllegalArgumentException("cannot redefine default charts for key " + key);
        }

        reports.put(key, parser.parseCharts(file));
        LOGGER.debug("loaded chart definitions from '{}'", file);
    }

    /**
     * Get the report for the given key.
     * 
     * @return a list of chart definitions; this list will be empty if the key is not found
     */
    public List<BaseChartDefinition> getChartDefinition(String key) {
        List<BaseChartDefinition> toReturn = reports.get(key);

        if (toReturn == null) {
            toReturn = java.util.Collections.emptyList();
        }

        return toReturn;
    }

    /**
     * Get the report for the given key filtering based on a given data set. Charts that are not
     * applicable to a host in the data set will not be included in the returned list.
     * 
     * @param dataSets the DataSets to match
     * @return the filtered list of chart definitions
     * @see DataDefinition#matchesHost(DataSet)
     */
    public List<BaseChartDefinition> getChartDefinition(String key, Iterable<? extends DataSet> dataSets) {
        List<BaseChartDefinition> report = reports.get(key);

        if (report == null) {
            return java.util.Collections.emptyList();
        }
        else {
            List<BaseChartDefinition> toReturn = new java.util.ArrayList<BaseChartDefinition>(report.size());
            // the charts actually used depend on the host
            // if any DataSet matches a defined host, show the report
            for (BaseChartDefinition chartDefinition : report) {
                dataset: for (DataSet data : dataSets) {
                    for (DataDefinition definition : chartDefinition.getData()) {
                        if (definition.matchesHost(data) && (definition.getMatchingTypes(data).size() > 0)) {
                            toReturn.add(chartDefinition);
                            break dataset;
                        }
                    }
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("reduced {} charts to {} for data {}: {}", new Object[] { report.size(), toReturn.size(),
                        dataSets, toReturn });
            }

            return toReturn;
        }
    }

    /**
     * <p>
     * Get the a custom report for the given key and data. Rather than creating a single chart with
     * a line/bar for each type, this function creates a chart for <em>each</em> type that matches
     * the given definition.
     * </p>
     * <p>
     * This function uses all charts in the given report so it is possible for this function to
     * create a large number of charts, especially if multiple DataTypes and/or fields are matched.
     * </p>
     * 
     * @param filterByData should the initial reports list be filtered by the given data? See
     *            {@link #getChartDefinition(String, Iterable)}.
     */
    public List<BaseChartDefinition> multiplexChartsAcrossTypes(String key, DataSet data, boolean filterByData) {
        List<BaseChartDefinition> chartDefinitions = null;

        if (filterByData) {
            chartDefinitions = getChartDefinition(key, java.util.Collections.singletonList(data));
        }
        else {
            chartDefinitions = getChartDefinition(key);
        }

        LOGGER.debug("multiplexing charts {} for dataset {} across types", chartDefinitions, data.getHostname());

        List<BaseChartDefinition> multiplexedChartDefinitions = new java.util.ArrayList<BaseChartDefinition>(
                10 * chartDefinitions.size());

        for (BaseChartDefinition chartDefinition : chartDefinitions) {
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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("multiplexed charts {} for dataset {} across types to {}", new Object[] { chartDefinitions,
                    data.getHostname(), multiplexedChartDefinitions });
        }

        return multiplexedChartDefinitions;
    }

    /**
     * <p>
     * Get the a custom report for the given key and data. Rather than creating a single chart with
     * a line/bar for each field, this function creates a chart for <em>each</em> field that matches
     * the given definition.
     * </p>
     * <p>
     * This function uses all charts in the given report so it is possible for this function to
     * create a large number of charts, especially if multiple DataTypes and/or fields are matched.
     * </p>
     * 
     * @param filterByData should the initial reports list be filtered by the given data? See
     *            {@link #getChartDefinition(String, Iterable)}.
     */
    public List<BaseChartDefinition> multiplexChartsAcrossFields(String key, DataSet data, boolean filterByData) {
        List<BaseChartDefinition> chartDefinitions = null;

        if (filterByData) {
            chartDefinitions = getChartDefinition(key, java.util.Collections.singletonList(data));
        }
        else {
            chartDefinitions = getChartDefinition(key);
        }

        LOGGER.debug("multiplexing charts {} for dataset {} across fields", chartDefinitions, data.getHostname());

        List<BaseChartDefinition> multiplexedChartDefinitions = new java.util.ArrayList<BaseChartDefinition>(
                10 * chartDefinitions.size());

        for (BaseChartDefinition chartDefinition : chartDefinitions) {
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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("multiplexed charts {} for dataset {} across fields to {}", new Object[] { chartDefinitions,
                    data.getHostname(), multiplexedChartDefinitions });
        }

        return multiplexedChartDefinitions;
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
}
