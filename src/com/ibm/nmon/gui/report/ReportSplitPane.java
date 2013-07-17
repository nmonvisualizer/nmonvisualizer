package com.ibm.nmon.gui.report;

import com.ibm.nmon.gui.main.ChartSplitPane;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.chart.BaseChartPanel;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

final class ReportSplitPane extends ChartSplitPane {
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

    void loadReport(File reportFile) throws Exception {
        int location = getDividerLocation();
        setTopComponent(null);
        dispose();

        try {
            gui.getReportCache().addReport("custom", reportFile.getAbsolutePath());

            // start with an empty list; ReportFrame will handle added the data
            reportPanel = new ReportPanel(gui, parent, "custom", new java.util.ArrayList<DataSet>(), multiplexMode);
            setTopComponent(reportPanel);

            reportPanel.addPropertyChangeListener("chart", summaryTable);
            reportPanel.addPropertyChangeListener("highlightedLine", this);
            reportPanel.addPropertyChangeListener("highlightedBar", this);

            reportPanel.setEnabled(true);
            setDividerLocation(location);
        }
        catch (Exception e) {
            // TODO need logger dialog
            setTopComponent(blank);
            validate();
            reportPanel = null;
        }
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
        return new String[] { "Hostname", "Data Type", "Metric", "Series Name", "Minimum", "Average", "Maximum",
                "Std Dev" };
    }
}
