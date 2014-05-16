package com.ibm.nmon.gui.main;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.chart.BaseChartPanel;
import com.ibm.nmon.gui.chart.DataTypeChartPanel;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

/**
 * View a chart for a specific set of fields from a {@link DataType}.
 * 
 * @see DataTypeChartPanel
 */
final class DataTypeView extends ChartSplitPane {
    private static final long serialVersionUID = -1143801614058577721L;

    private final DataTypeChartPanel chartPanel;

    DataTypeView(NMONVisualizerGui gui) {
        super(gui, gui.getMainFrame());

        chartPanel = new DataTypeChartPanel(gui);
        chartPanel.setBorder(Styles.createTopLineBorder(this));

        // link chart and summary table; ChartSplitPane will handle highlighted chart lines
        chartPanel.addPropertyChangeListener("chart", summaryTable);
        chartPanel.addPropertyChangeListener("highlightedLine", this);

        // ensure ChartSplitPane forwards these events
        chartPanel.addPropertyChangeListener("chart", this);
        chartPanel.addPropertyChangeListener("annotation", this);

        gui.addDataSetListener(this);

        setTopComponent(chartPanel);

        setEnabled(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            super.setEnabled(enabled);

            chartPanel.setEnabled(enabled);
        }
    }

    @Override
    protected BaseChartPanel getChartPanel() {
        return chartPanel;
    }

    void setData(DataSet data, DataType type) {
        chartPanel.setData(data, type);
    }

    void setData(DataSet data, DataType type, String field) {
        chartPanel.setData(data, type, java.util.Collections.singletonList(field));
    }

    @Override
    protected void saveCharts(String directory) {
        // null => saveChart will create a default name
        chartPanel.saveChart(directory, null);
    }

    @Override
    public void dataAdded(DataSet data) {}

    @Override
    public void dataRemoved(DataSet data) {
        DataSet current = chartPanel.getData();

        if ((current != null) && current.equals(data)) {
            chartPanel.clearChart();
        }
        // current == null => chart already clear
    }

    @Override
    public void dataChanged(DataSet data) {}

    @Override
    public void dataCleared() {
        chartPanel.clearChart();
    }
}
