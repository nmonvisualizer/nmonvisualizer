package com.ibm.nmon.gui.main;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import com.ibm.nmon.gui.report.ReportPanel;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.parser.ChartDefinitionParser;
import com.ibm.nmon.chart.definition.BaseChartDefinition;

import com.ibm.nmon.gui.chart.BaseChartPanel;

/**
 * View a set of summary charts for a specific {@link DataSet}.
 * 
 * @see ReportPanel
 */
final class DataSetView extends ChartSplitPane {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataSetView.class);

    private final Map<String, ReportPanel> reportPanels;

    private ReportPanel currentReport;

    private final List<BaseChartDefinition> reportDefinitions;

    private String lastCommonTabName;

    DataSetView(NMONVisualizerGui gui) {
        super(gui);

        reportPanels = new java.util.HashMap<String, ReportPanel>();

        ChartDefinitionParser parser = new ChartDefinitionParser();
        List<BaseChartDefinition> reports = null;

        try {
            reports = parser.parseCharts(ReportPanel.class
                    .getResourceAsStream("/com/ibm/nmon/report/dataset_report.xml"));
        }
        catch (Exception e) {
            LOGGER.error("cannot parse report definition xml", e);
            reports = java.util.Collections.emptyList();
        }

        reportDefinitions = reports;

        for (DataSet data : gui.getDataSets()) {
            dataAdded(data);
        }

        gui.addDataSetListener(this);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            super.setEnabled(enabled);

            if (currentReport != null) {
                currentReport.setEnabled(enabled);
            }
        }
    }

    @Override
    protected BaseChartPanel getChartPanel() {
        return currentReport == null ? null : currentReport.getChartPanel();
    }

    void setData(DataSet data) {
        ReportPanel forData = reportPanels.get(data.getHostname());
        String lastTabName = null;

        if (forData != currentReport) {
            if (currentReport != null) {
                currentReport.setEnabled(false);
                lastTabName = currentReport.getTitleAt(currentReport.getSelectedIndex());
            }

            // sync all report panel's divider locations
            int location = getDividerLocation();

            currentReport = forData;

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
            if (!found && (lastCommonTabName != null)) {
                for (int i = 0; i < currentReport.getTabCount(); i++) {
                    if (currentReport.getTitleAt(i).equals(lastCommonTabName)) {
                        if (currentReport.getSelectedIndex() != i) {
                            currentReport.setSelectedIndex(i);
                        }

                        break;
                    }
                }
            }
            // otherwise, the currently selected tab (which starts at 0 for new report panels) will
            // not change

            setTopComponent(currentReport);
            setDividerLocation(location);

            if (isEnabled()) {
                currentReport.setEnabled(true);
            }
        }
        // else correct report panel and tab already selected
    }

    @Override
    void saveCharts(String directory) {
        if (currentReport != null) {
            currentReport.saveAllCharts(directory);
        }
    }

    @Override
    public void dataAdded(DataSet data) {
        if (!reportPanels.containsKey(data.getHostname())) {
            // create the report panel for the DataSet and make sure it sends events to the table
            ReportPanel reportPanel = new ReportPanel(gui, reportDefinitions, data);

            reportPanel.addPropertyChangeListener("chart", summaryTable);
            reportPanel.addPropertyChangeListener("highlightedLine", this);
            reportPanel.addPropertyChangeListener("highlightedBar", this);

            reportPanel.setEnabled(false);

            reportPanels.put(data.getHostname(), reportPanel);
        }
        // assume existing report panel will handle updates to an existing DataSet
    }

    @Override
    public void dataRemoved(DataSet data) {
        ReportPanel forData = reportPanels.remove(data.getHostname());

        if (forData != null) {
            forData.dispose();

            if (forData == currentReport) {
                currentReport = null;
            }
        }
    }

    @Override
    public void dataChanged(DataSet data) {
        ReportPanel forData = reportPanels.get(data.getHostname());

        if (forData != null) {
            forData.resetReport();
        }
    }

    @Override
    public void dataCleared() {
        for (ReportPanel report : reportPanels.values()) {
            report.dispose();
        }

        reportPanels.clear();
        currentReport = null;
    }

    @Override
    protected String[] getDefaultColumns() {
        return new String[] { "Series Name", "Minimum", "Average", "Maximum", "Std Dev" };
    }
}
