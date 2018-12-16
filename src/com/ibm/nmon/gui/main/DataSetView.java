package com.ibm.nmon.gui.main;

import java.util.Map;

import com.ibm.nmon.gui.report.ReportPanel;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.chart.BaseChartPanel;
import com.ibm.nmon.report.ReportCache;

/**
 * View a set of summary charts for a specific {@link DataSet}.
 * 
 * @see ReportPanel
 */
final class DataSetView extends ChartSplitPane {
    private static final long serialVersionUID = 6066688638520302421L;

    private final Map<String, ReportPanel> reportPanels;

    private ReportPanel currentReport;

    private String lastCommonTabName;

    DataSetView(NMONVisualizerGui gui) {
        super(gui, gui.getMainFrame());

        reportPanels = new java.util.HashMap<String, ReportPanel>();

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
    protected void saveCharts(String directory) {
        if (currentReport != null) {
            currentReport.saveAllCharts(directory);
        }
    }

    @Override
    public void dataAdded(DataSet data) {
        if (!reportPanels.containsKey(data.getHostname())) {
            // create the report panel for the DataSet and make sure it sends events to the table
            ReportPanel reportPanel = new ReportPanel(gui, ReportCache.DEFAULT_DATASET_CHARTS_KEY, data);

            reportPanel.addPropertyChangeListener("chart", summaryTable);
            reportPanel.addPropertyChangeListener("highlightedLine", this);
            reportPanel.addPropertyChangeListener("highlightedBar", this);

            // ensure ChartSplitPane forwards these events
            reportPanel.addPropertyChangeListener("chart", this);
            reportPanel.addPropertyChangeListener("annotation", this);

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
        setTopComponent(null);
    }
}
