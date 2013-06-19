package com.ibm.nmon.gui.main;

import org.slf4j.Logger;

import java.util.List;

import java.awt.BorderLayout;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import javax.swing.SwingConstants;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;

import org.jfree.chart.plot.CategoryPlot;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.interval.IntervalListener;

import com.ibm.nmon.parser.ChartDefinitionParser;

import com.ibm.nmon.chart.definition.BaseChartDefinition;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.chart.BaseChartPanel;
import com.ibm.nmon.gui.chart.builder.ChartBuilderPlugin;

import com.ibm.nmon.gui.report.ReportPanel;

/**
 * ChartSplitPane showing summary charts for either all defined intervals or a single interval. The
 * choice is determined by a check box.
 * 
 * @see ReportPanel
 */
final class SummaryView extends ChartSplitPane implements IntervalListener {
    protected final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SummaryView.class);

    private final ReportPanel singleIntervalReport;
    private final ReportPanel allIntervalsReport;

    private final JCheckBox allIntervals;

    private String lastCommonTabName;

    public SummaryView(NMONVisualizerGui gui) {
        super(gui);

        ChartDefinitionParser parser = new ChartDefinitionParser();

        List<BaseChartDefinition> reports = null;

        try {
            reports = parser.parseCharts(ReportPanel.class
                    .getResourceAsStream("/com/ibm/nmon/report/summary_single_interval.xml"));
        }
        catch (Exception e) {
            LOGGER.error("cannot parse report definition xml", e);
            reports = java.util.Collections.emptyList();
        }

        singleIntervalReport = new ReportPanel(gui, reports);
        singleIntervalReport.setBorder(null); // make consistent with addBorderIfNecessary

        singleIntervalReport.addPlugin(new ChartBuilderPlugin() {
            @Override
            public void configureChart(JFreeChart chart) {
                if (chart.getPlot() instanceof CategoryPlot) {
                    // assume bar names will usually be hostnames
                    // draw them on the chart at a 45 degree angle
                    CategoryPlot plot = (CategoryPlot) chart.getPlot();
                    plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
                }
            }
        });

        try {
            reports = parser.parseCharts(ReportPanel.class
                    .getResourceAsStream("/com/ibm/nmon/report/summary_all_intervals.xml"));
        }
        catch (Exception e) {
            LOGGER.error("cannot parse report definition xml", e);
            reports = java.util.Collections.emptyList();
        }

        allIntervalsReport = new ReportPanel(gui, reports);
        allIntervalsReport.setBorder(null); // make consistent with addBorderIfNecessary

        allIntervals = new JCheckBox("Graph All Intervals");
        allIntervals.setEnabled(false);
        allIntervals.setSelected(false);
        allIntervals.setFont(Styles.LABEL);
        allIntervals.setHorizontalAlignment(SwingConstants.RIGHT);
        allIntervals.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        allIntervals.setBorderPainted(true);
        allIntervals.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleReports();
            }
        });

        setTopComponent(singleIntervalReport);
        addBorderIfNecessary();

        setEnabled(false);

        gui.addDataSetListener(this);
        gui.getIntervalManager().addListener(this);

        // assuming specific layout for NMONVisualizerGui
        JPanel right = (JPanel) ((javax.swing.JSplitPane) gui.getMainFrame().getContentPane()).getRightComponent();
        JPanel top = (JPanel) right.getComponent(0);
        top.add(allIntervals, BorderLayout.LINE_START);

        ActionMap actions = allIntervals.getActionMap();
        InputMap inputs = allIntervals.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        actions.put("allIntervals", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                allIntervals.doClick();
            }
        });

        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "allIntervals");

        allIntervalsReport.addPropertyChangeListener("chart", summaryTable);
        allIntervalsReport.addPropertyChangeListener("highlightedLine", this);
        allIntervalsReport.addPropertyChangeListener("highlightedBar", this);
        allIntervalsReport.addPropertyChangeListener("highlightedIntervalLine", this);

        singleIntervalReport.addPropertyChangeListener("chart", summaryTable);
        singleIntervalReport.addPropertyChangeListener("highlightedLine", this);
        singleIntervalReport.addPropertyChangeListener("highlightedBar", this);
    }

    @Override
    protected BaseChartPanel getChartPanel() {
        if (allIntervalsReport.isEnabled()) {
            return allIntervalsReport.getChartPanel();
        }
        else {
            return singleIntervalReport.getChartPanel();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            super.setEnabled(enabled);

            if (enabled) {
                if (allIntervals.isSelected()) {
                    allIntervalsReport.setEnabled(true);
                }
                else {
                    singleIntervalReport.setEnabled(true);
                }

                if (gui.getIntervalManager().getIntervalCount() > 0) {
                    allIntervals.setEnabled(true);
                }
            }
            else {
                allIntervalsReport.setEnabled(false);
                singleIntervalReport.setEnabled(false);
                allIntervals.setEnabled(false);
            }
        }
    }

    @Override
    void saveCharts(String directory) {
        if (allIntervalsReport.isEnabled()) {
            allIntervalsReport.saveAllCharts(directory);
        }
        else {
            singleIntervalReport.saveAllCharts(directory);
        }
    }

    @Override
    public void dataAdded(DataSet data) {
        singleIntervalReport.addData(data);
        allIntervalsReport.addData(data);

        int index = singleIntervalReport.getPreviousTab();

        if (index != -1) {
            singleIntervalReport.setSelectedIndex(index);
        }
        else {
            singleIntervalReport.setSelectedIndex(0);
        }

        index = allIntervalsReport.getPreviousTab();

        if (index != -1) {
            allIntervalsReport.setSelectedIndex(index);
        }
        else {
            allIntervalsReport.setSelectedIndex(0);
        }

        addBorderIfNecessary();
    }

    @Override
    public void dataRemoved(DataSet data) {
        singleIntervalReport.removeData(data);

        allIntervalsReport.removeData(data);

        updateLastTab();
        addBorderIfNecessary();
    }

    @Override
    public void dataChanged(DataSet data) {
        singleIntervalReport.resetReport();
        allIntervalsReport.resetReport();
    }

    @Override
    public void dataCleared() {
        singleIntervalReport.clearData();
        allIntervalsReport.clearData();

        addBorderIfNecessary();
    }

    public void intervalAdded(Interval interval) {
        if (isEnabled()) {
            allIntervals.setEnabled(true);
        }
        // else setEnabled() will handle enabling allIntervals if necessary
    }

    public void intervalRemoved(Interval interval) {
        if (gui.getIntervalManager().getIntervalCount() == 0) {
            intervalsCleared();
        }
    }

    public void intervalsCleared() {
        if (isEnabled()) {
            allIntervals.setEnabled(false);
            allIntervals.setSelected(false);

            toggleReports();
        }
    }

    public void currentIntervalChanged(Interval interval) {}

    public void intervalRenamed(Interval interval) {}

    private void toggleReports() {
        // keep the same divider location when changing the top component
        int location = getDividerLocation();

        if (allIntervals.isSelected()) {
            singleIntervalReport.setEnabled(false);
            allIntervalsReport.setEnabled(true);

            updateLastTab();
            setTopComponent(allIntervalsReport);
        }
        else {
            allIntervalsReport.setEnabled(false);
            singleIntervalReport.setEnabled(true);

            updateLastTab();
            setTopComponent(singleIntervalReport);
        }

        setDividerLocation(location);

        revalidate();
    }

    private void addBorderIfNecessary() {
        // ensure there is a separator line between the top panel and the report
        // if there are no tabs
        if (allIntervalsReport.getTabCount() == 0) {
            if (allIntervalsReport.getBorder() == null) {
                allIntervalsReport.setBorder(Styles.createTopLineBorder(this));
            }
        }
        else {
            if (allIntervalsReport.getBorder() != null) {
                allIntervalsReport.setBorder(null);
            }
        }

        if (singleIntervalReport.getTabCount() == 0) {
            if (singleIntervalReport.getBorder() == null) {
                singleIntervalReport.setBorder(Styles.createTopLineBorder(this));
            }
        }
        else {
            if (singleIntervalReport.getBorder() != null) {
                singleIntervalReport.setBorder(null);
            }
        }
    }

    private void updateLastTab() {
        ReportPanel currentReport = null;

        // this function is only called after selection is made
        // so get the previous report's selected index
        if (allIntervals.isSelected()) {
            currentReport = singleIntervalReport;
        }
        else {
            currentReport = allIntervalsReport;
        }

        int index = currentReport.getSelectedIndex();
        String lastTabName = null;

        if (index != -1) {
            lastTabName = currentReport.getTitleAt(index);
        }

        // now update the _other_ report (the currently selected one)
        // this is the opposite of the above if
        if (allIntervals.isSelected()) {
            currentReport = allIntervalsReport;
        }
        else {
            currentReport = singleIntervalReport;
        }

        boolean found = false;

        // attempt to select the same tab that was selected on the previous report
        if (lastTabName != null) {
            for (int i = 0; i < currentReport.getTabCount(); i++) {
                if (currentReport.getTitleAt(i).equals(lastTabName)) {
                    if (currentReport.getSelectedIndex() != i) {
                        currentReport.setSelectedIndex(i);
                    }

                    lastCommonTabName = lastTabName;
                    found = true;
                    break;
                }
            }
        }

        // tab with same name not found, attempt to find the last common name
        if (!found) {
            for (int i = 0; i < currentReport.getTabCount(); i++) {
                if (currentReport.getTitleAt(i).equals(lastCommonTabName)) {
                    if (currentReport.getSelectedIndex() != i) {
                        currentReport.setSelectedIndex(i);
                    }

                    break;
                }
            }
        }

        // otherwise, the currently selected tab will not change
    }

    @Override
    protected String[] getDefaultColumns() {
        return new String[] { "Series Name", "Minimum", "Average", "Maximum", "Std Dev" };
    }
}
