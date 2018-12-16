package com.ibm.nmon.gui.report;

import com.ibm.nmon.gui.main.ChartSplitPane;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.chart.BaseChartPanel;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Display a custom report.
 * 
 * @see ReportFrame
 */
final class ReportSplitPane extends ChartSplitPane {
    private static final long serialVersionUID = -8401367823499645439L;

    private final JFrame parent;

    private final JPanel blank;
    private ReportPanel reportPanel;

    private ReportPanel.MultiplexMode multiplexMode;

    ReportSplitPane(NMONVisualizerGui gui, JFrame parent) {
        super(gui, parent);

        this.parent = parent;
        this.multiplexMode = ReportPanel.MultiplexMode.NONE;

        blank = new JPanel();
        blank.setBackground(java.awt.Color.WHITE);
        blank.setBorder(Styles.createTopLineBorder(blank));

        setTopComponent(blank);
    }

    boolean hasReport() {
        return getTopComponent() != blank;
    }

    void loadReport(File reportFile) throws IOException {
        try {
            gui.getReportCache().addReport("custom", reportFile.getAbsolutePath());
        }
        catch (IOException ioe) {
            throw ioe;
        }

        createReportPanel("custom");
    }

    void loadDefaultDataSetReport() {
        createReportPanel(com.ibm.nmon.report.ReportCache.DEFAULT_DATASET_CHARTS_KEY);
    }

    void loadDefaultSummaryReport() {
        createReportPanel(com.ibm.nmon.report.ReportCache.DEFAULT_SUMMARY_CHARTS_KEY);
    }

    void loadDefaultIOStatReport() {
        createReportPanel(com.ibm.nmon.report.ReportCache.DEFAULT_IOSTAT_CHARTS_KEY);
    }

    void loadDefaultIOStatDiskDataReport() {
        createReportPanel(com.ibm.nmon.report.ReportCache.DEFAULT_IOSTAT_DISKDATA_CHARTS_KEY);
    }

    private void createReportPanel(String cacheKey) {
        setTopComponent(null);
        dispose();

        int location = getDividerLocation();

        // start with an empty list; ReportFrame will handle adding the data
        reportPanel = new ReportPanel(gui, parent, cacheKey, new java.util.ArrayList<DataSet>(), multiplexMode);
        setTopComponent(reportPanel);

        reportPanel.addPropertyChangeListener("chart", summaryTable);
        reportPanel.addPropertyChangeListener("highlightedLine", this);
        reportPanel.addPropertyChangeListener("highlightedBar", this);

        // ensure ChartSplitPane forwards these events
        reportPanel.addPropertyChangeListener("chart", this);
        reportPanel.addPropertyChangeListener("annotation", this);

        reportPanel.setEnabled(true);
        setDividerLocation(location);
    }

    void setData(Iterable<? extends DataSet> dataSets) {
        if (reportPanel != null) {
            reportPanel.setData(dataSets);
        }
    }

    void setMultiplexMode(ReportPanel.MultiplexMode multiplexMode) {
        if (reportPanel != null) {
            reportPanel.setMultiplexMode(multiplexMode);
        }
    }

    // data selection is handled by ReportFrame's JList of systems
    @Override
    public void dataAdded(DataSet data) {}

    @Override
    public void dataRemoved(DataSet data) {}

    @Override
    public void dataChanged(DataSet data) {}

    @Override
    public void dataCleared() {}

    void dispose() {
        if (reportPanel != null) {
            reportPanel.dispose();
            reportPanel = null;
        }
    }

    @Override
    protected BaseChartPanel getChartPanel() {
        return reportPanel == null ? null : reportPanel.getChartPanel();
    }

    @Override
    protected void saveCharts(String directory) {
        if (reportPanel != null) {
            reportPanel.saveAllCharts(directory);
        }
    }

    @Override
    protected String[] getDefaultColumns() {
        return new String[] { "Hostname", "Data Type", "Metric", "Series Name", "Minimum", "Average",
                "Weighted Average", "Maximum", "Std Dev" };
    }
}
