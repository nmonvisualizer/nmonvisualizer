package com.ibm.nmon.gui.report;

import org.slf4j.Logger;

import java.util.BitSet;
import java.util.List;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.jfree.chart.JFreeChart;

import com.ibm.nmon.analysis.Statistic;

import com.ibm.nmon.chart.definition.BarChartDefinition;
import com.ibm.nmon.chart.definition.BaseChartDefinition;
import com.ibm.nmon.chart.definition.IntervalChartDefinition;
import com.ibm.nmon.chart.definition.LineChartDefinition;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.definition.DataDefinition;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.chart.BaseChartPanel;
import com.ibm.nmon.gui.chart.ChartFactory;
import com.ibm.nmon.gui.chart.IntervalChartPanel;
import com.ibm.nmon.gui.chart.LineChartPanel;
import com.ibm.nmon.gui.chart.BarChartPanel;

import com.ibm.nmon.gui.chart.builder.ChartBuilderPlugin;

import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.util.ItemProgressDialog;

import com.ibm.nmon.interval.IntervalListener;
import com.ibm.nmon.interval.Interval;

/**
 * <p>
 * JTabbed pane for displaying a set of related charts. Charts are defined using
 * {@link BaseChartDefinition}. Each chart will appear on a tab named by
 * {@link BaseChartDefinition#getShortName() getShortName()}.
 * </p>
 * 
 * <p>
 * This class listens for {@link IntervalListener interval} events as well as time zone and
 * granularity changes. New {@link DataSet data} can be added to or removed from reports at run time
 * (the given chart definitions should display values from more than one data set).
 * </p>
 * 
 * <p>
 * This class also ensures that tabs will only be created for charts that are supported by the given
 * data sets. This allows multiple chart definitions with the same short name to be passed in if
 * they match different data sets (i.e. different hostnames or operating systems).
 * </p>
 */
public final class ReportPanel extends JTabbedPane implements PropertyChangeListener, IntervalListener {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ReportPanel.class);

    private final NMONVisualizerGui gui;

    private final List<DataSet> dataSets;

    private final List<BaseChartDefinition> chartDefinitions;
    private List<BaseChartDefinition> chartsInUse;

    private final ChartFactory chartFactory;

    private final BitSet chartNeedsUpdate;

    // ignore chart updates when tabs are being built
    private boolean buildingTabs;

    private int previousTab = -1;

    public ReportPanel(NMONVisualizerGui gui, List<BaseChartDefinition> reports, DataSet data) {
        this(gui, reports, java.util.Collections.singletonList(data));
    }

    public ReportPanel(NMONVisualizerGui gui, List<BaseChartDefinition> reports) {
        this(gui, reports, new java.util.ArrayList<DataSet>());
    }

    private ReportPanel(NMONVisualizerGui gui, List<BaseChartDefinition> reports, List<DataSet> dataSets) {
        super();

        this.chartFactory = new ChartFactory(gui);

        // will not have any effect unless setOpaque(true) - see build tabs
        setBackground(java.awt.Color.WHITE);

        this.gui = gui;
        this.dataSets = dataSets;
        this.chartDefinitions = reports;
        this.chartsInUse = new java.util.ArrayList<BaseChartDefinition>(reports.size());

        chartNeedsUpdate = new BitSet(reports.size());

        chartNeedsUpdate.set(0, chartNeedsUpdate.size(), true);

        buildTabs(gui);

        addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                // no need to update the chart if the tabs are still being built
                if (!buildingTabs) {
                    int idx = getSelectedIndex();

                    if (idx != -1) {
                        if (!updateChart()) {
                            // still need to notify listeners that the chart is now showing
                            firePropertyChange("chart", null, getChartPanel(idx).getDataset());
                        }

                        if (previousTab != -1) {
                            getChartPanel(previousTab).setEnabled(false);
                        }

                        getChartPanel(idx).setEnabled(true);
                        previousTab = idx;
                    }

                }
            }
        });

        setEnabled(false);

        gui.getIntervalManager().addListener(this);
        gui.addPropertyChangeListener("granularity", this);
        gui.addPropertyChangeListener("timeZone", this);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            super.setEnabled(enabled);

            if (!chartsInUse.isEmpty()) {
                int idx = getSelectedIndex();

                if (enabled) {
                    if (idx != -1) {
                        if (!updateChart()) {
                            // still need to notify listeners that the chart is now showing
                            firePropertyChange("chart", null, getChartPanel(idx).getDataset());
                        }
                    }
                }
                else {
                    if (idx != -1) {
                        // notify listeners that the chart is now showing
                        getChartPanel().clearChart();
                        // ensure chart is recreated when re-enabled
                        chartNeedsUpdate.set(idx);
                        firePropertyChange("chart", null, null);
                    }
                }

                if (idx != -1) {
                    getChartPanel(idx).setEnabled(enabled);
                    previousTab = idx;
                }

                // leave the listeners enabled
                // the heavy work done in the listeners is in updateChart() which checks for enabled
                // too
            }
        }
    }

    public void addData(DataSet data) {
        dataSets.add(data);
        java.util.Collections.sort(dataSets);
        buildTabs(gui);
        resetReport();
    }

    public void removeData(DataSet data) {
        dataSets.remove(data);
        java.util.Collections.sort(dataSets);
        buildTabs(gui);
        resetReport();
    }

    public void clearData() {
        dataSets.clear();
        buildTabs(gui);
        resetReport();
    }

    // mark all charts as invalid; update the current one
    public void resetReport() {
        chartNeedsUpdate.set(0, chartNeedsUpdate.size(), true);
        updateChart();
    }

    // update the current chart if enabled
    // note that clearing / setting the chart fires a property change event
    // return false if this did not happen so callers can fire the event regardless
    private boolean updateChart() {
        if (isEnabled() && (getTabCount() != 0)) {
            int index = getSelectedIndex();

            if ((index >= 0) && !chartsInUse.isEmpty()) {
                BaseChartPanel chartPanel = getChartPanel(index);

                if (chartNeedsUpdate.get(index)) {
                    if (dataSets.isEmpty()) {
                        chartPanel.clearChart();
                    }
                    else {
                        createChart(index);
                    }

                    chartNeedsUpdate.clear(index);
                    return true;
                }
            }
        }

        return false;
    }

    public BaseChartPanel getChartPanel() {
        int index = getSelectedIndex();

        if (index == -1) {
            return null;
        }
        else {
            return getChartPanel(index);
        }
    }

    private BaseChartPanel getChartPanel(int index) {
        return (BaseChartPanel) getComponentAt(index);
    }

    public int getPreviousTab() {
        return previousTab;
    }

    public void addPlugin(ChartBuilderPlugin plugin) {
        chartFactory.addPlugin(plugin);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("granularity".equals(evt.getPropertyName())) {
            int newGranularity = (Integer) evt.getNewValue();

            chartFactory.setGranularity(newGranularity);

            // always update line charts on granularity changes
            // bar charts and interval line charts do not need to be updated unless the stat is
            // granularity max
            for (int i = 0; i < chartsInUse.size(); i++) {
                BaseChartDefinition chartDefinition = chartsInUse.get(i);

                if (chartDefinition.getClass().equals(IntervalChartDefinition.class)) {
                    for (DataDefinition definition : ((IntervalChartDefinition) chartsInUse.get(i)).getLines()) {
                        if (definition.getStatistic() == Statistic.GRANULARITY_MAXIMUM) {
                            chartNeedsUpdate.set(i);
                            break;
                        }
                    }
                }
                else if (chartDefinition.getClass().equals(BarChartDefinition.class)) {
                    for (DataDefinition definition : ((BarChartDefinition) chartsInUse.get(i)).getCategories()) {
                        if (definition.getStatistic() == Statistic.GRANULARITY_MAXIMUM) {
                            chartNeedsUpdate.set(i);
                            break;
                        }
                    }
                }
                else {
                    chartNeedsUpdate.set(i);
                }

                updateChart();
            }
        }
        else if ("timeZone".equals(evt.getPropertyName())) {
            // assume bar charts do not need to be updated and linecharts handle this internally
            // interval charts may have unnamed intervals that need to be re-displayed
            // just recreate the chart
            updateIntervalCharts();
        }
        else if ("chart".equals(evt.getPropertyName())) {
            // called by chart panels when the chart changes
            // propagate chart events to listeners
            firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
        }
        else if (evt.getPropertyName().startsWith("highlighted")) {
            // called by chart panels when an element is highlighted
            // propagate chart events to listeners
            firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
        }
    }

    @Override
    public void intervalAdded(Interval interval) {
        updateIntervalCharts();
    }

    @Override
    public void intervalRemoved(Interval interval) {
        updateIntervalCharts();
    }

    @Override
    public void intervalsCleared() {
        updateIntervalCharts();
    }

    @Override
    public void currentIntervalChanged(Interval interval) {
        chartFactory.setInterval(interval);

        // update non-interval charts
        for (int i = 0; i < chartsInUse.size(); i++) {
            if (!chartsInUse.get(i).getClass().equals(IntervalChartDefinition.class)) {
                chartNeedsUpdate.set(i);
            }
        }

        updateChart();
    }

    @Override
    public void intervalRenamed(Interval interval) {
        updateIntervalCharts();
    }

    @Override
    public void removeAll() {
        if (!chartsInUse.isEmpty()) {
            for (int i = 0; i < getTabCount(); i++) {
                // buildTabs() adds each chart as a listener, so remove it when the tabs change
                BaseChartPanel chartPanel = getChartPanel(i);

                chartPanel.setEnabled(false);
                chartPanel.clearChart();
                chartPanel.removePropertyChangeListener(this);
            }
        }

        super.removeAll();
    }

    public void dispose() {
        gui.getIntervalManager().removeListener(this);
        gui.removePropertyChangeListener("granularity", this);
        gui.removePropertyChangeListener("timeZone", this);

        // clean up references to charts
        removeAll();
        chartsInUse.clear();
    }

    private void updateIntervalCharts() {
        // interval charts need to be updated
        for (int i = 0; i < chartsInUse.size(); i++) {
            if (chartsInUse.get(i).getClass().equals(IntervalChartDefinition.class)) {
                chartNeedsUpdate.set(i);
            }
        }

        updateChart();
    }

    // no need to recalculate granularity on data changes because NMONVisualizerApp recalculates min
    // and max system times on data changes, which also changes the interval if it is the default

    private void buildTabs(NMONVisualizerGui gui) {
        buildingTabs = true;
        // note remove all needs to know is charts existed previously, order matters here
        removeAll();
        chartsInUse.clear();

        if (chartDefinitions.isEmpty()) {
            addTab("No Charts", createNoReportsLabel("No Charts Defined!"));
        }
        else {
            if (dataSets.isEmpty()) {
                setOpaque(true);
                return;
            }
            else {
                setOpaque(false);
            }

            chartsInUse = chartFactory.getChartsForData(chartDefinitions, dataSets);

            if (chartsInUse.isEmpty()) {
                addTab("No Charts", createNoReportsLabel("No Charts for Currently Parsed Data!"));
            }
            else {
                for (BaseChartDefinition report : chartsInUse) {
                    BaseChartPanel chartPanel = null;

                    if (report.getClass() == LineChartDefinition.class) {
                        chartPanel = new LineChartPanel(gui);
                    }
                    else if (report.getClass() == IntervalChartDefinition.class) {
                        chartPanel = new IntervalChartPanel(gui);
                    }
                    else if (report.getClass() == BarChartDefinition.class) {
                        chartPanel = new BarChartPanel(gui);
                    }
                    else {
                        LOGGER.error("cannot create chart panel for {} ({})", report.getShortName(), report.getClass()
                                .getSimpleName());
                    }

                    // this class will receive each chart's change events and forward them rather
                    // than
                    // expose each chart as a separate listener
                    chartPanel.addPropertyChangeListener(this);
                    addTab(report.getShortName(), chartPanel);
                }
            }
        }

        buildingTabs = false;
    }

    private void createChart(int index) {
        BaseChartDefinition definition = chartsInUse.get(index);

        JFreeChart chart = chartFactory.createChart(definition, dataSets);

        // setChart will fire the event that updates the data table
        getChartPanel(index).setChart(chart);
    }

    public void saveAllCharts(final String directory) {
        final ItemProgressDialog progress = new ItemProgressDialog(gui, "Saving Charts...", getTabCount());

        // This code is a mess of things running in and out of the Swing Event Thread mostly to
        // allow the progress dialog to be modal. If it was not modal, other issues would arise if
        // users could continue to click on the UI while this code is trying to change tabs, etc.
        // The current implementation seems to work correctly WRT updating the progress bar and not
        // causing any exceptions due to events firing when tabs / charts are changed. Why this is,
        // however, is somewhat of a multi-threaded mystery.
        Thread saver = new Thread(new Runnable() {
            @Override
            public void run() {
                final int originalTab = getSelectedIndex();

                // start modal dialog must not happen directly in the Swing thread or no other code
                // will run until it is closed
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisible(true);
                    }
                });

                for (int i = 0; i < getTabCount(); i++) {
                    String name = chartsInUse.get(i).getShortName();

                    if (dataSets.size() == 1) {
                        name += '-' + dataSets.get(0).getHostname();
                    }

                    final String finalName = name;

                    // invokeLater() ensures the name is set before the progress is updated
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progress.setCurrentItem(finalName);
                        }
                    });

                    final int n = i;

                    // wait here to ensure the chart actually exists before trying to save it
                    // save is in the event thread too since the chart object is manipulated when
                    // saved
                    // blocking the event thread is OK since there is a modal dialog anyway
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                setSelectedIndex(n);
                                getChartPanel(n).saveChart(directory, finalName);
                            }
                        });
                    }
                    catch (Exception e) {
                        LOGGER.warn("error saving chart " + finalName, e);
                        continue;
                    }

                    // updating the progress does not work correctly when put in the previous
                    // invokeLater() call
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progress.updateProgress();
                        }
                    });
                } // end for

                // wait here so that the progress dialog finishes updating before disappearing
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            setSelectedIndex(originalTab);
                            progress.dispose();
                        }
                    });
                }
                catch (Exception e) {
                    LOGGER.warn("error closing progress dialog", e);
                }
            }
        });

        saver.start();
    }

    private JLabel createNoReportsLabel(String toDisplay) {
        JLabel label = new JLabel(toDisplay);
        label.setFont(Styles.LABEL_ERROR.deriveFont(Styles.LABEL_ERROR.getSize() * 1.5f));
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        label.setBackground(java.awt.Color.WHITE);
        label.setForeground(Styles.ERROR_COLOR);
        label.setOpaque(true);

        return label;
    }
}
