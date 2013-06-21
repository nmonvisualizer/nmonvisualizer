package com.ibm.nmon.gui.main;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.JSplitPane;

import com.ibm.nmon.data.DataSetListener;

import com.ibm.nmon.gui.chart.BaseChartPanel;
import com.ibm.nmon.gui.chart.summary.ChartSummaryPanel;

/**
 * <p>
 * A split view that displays a chart on top and a linked table of charted values on the bottom.
 * Subclasses are responsible for updating the table when the chart changes.
 * </p>
 * 
 * <p>
 * This class listens for selection events from the table and highlights the associated value on the
 * chart. For the other direction of event flow, subclasses should link property change events on
 * their chart to this class so that highlight selections made on the chart updated the selected row
 * of the table.
 * </p>
 * 
 * @see ChartSummaryPanel
 * @see BaseChartPanel#highlightElement(int, int)
 */
abstract class ChartSplitPane extends JSplitPane implements PropertyChangeListener, DataSetListener {
    protected final NMONVisualizerGui gui;

    protected final ChartSummaryPanel summaryTable;

    private boolean changeInProgress;

    protected ChartSplitPane(NMONVisualizerGui gui) {
        super(JSplitPane.VERTICAL_SPLIT);

        // summary panel gets a little of the extra space
        setResizeWeight(.75);
        setOneTouchExpandable(true);
        setBorder(null);

        this.gui = gui;
        summaryTable = new ChartSummaryPanel(gui, getDefaultColumns());
        changeInProgress = false;

        setBottomComponent(summaryTable);

        summaryTable.addPropertyChangeListener("selectedRows", this);
        summaryTable.addPropertyChangeListener("rowVisible", this);
    }

    public void displayTableColumnChooser() {
        summaryTable.displayTableColumnChooser();
    }

    protected String[] getDefaultColumns() {
        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (changeInProgress) {
            return;
        }

        if ("highlightedLine".equals(evt.getPropertyName())) {
            changeInProgress = true;

            Integer newRow = (Integer) evt.getNewValue();

            if (newRow == null) {
                summaryTable.clearSelection();
            }
            else {
                summaryTable.selectRow(newRow);
            }

            changeInProgress = false;
        }
        else if ("highlightedBar".equals(evt.getPropertyName())) {
            changeInProgress = true;

            int[] newValues = (int[]) evt.getNewValue();

            if (newValues == null) {
                summaryTable.clearSelection();
            }
            else {
                summaryTable.selectRow(newValues[0], newValues[1]);
            }

            changeInProgress = false;
        }
        else if ("highlightedIntervalLine".equals(evt.getPropertyName())) {
            changeInProgress = true;

            Integer newRow = (Integer) evt.getNewValue();

            if (newRow == null) {
                summaryTable.clearSelection();
            }
            else {
                summaryTable.selectRow(newRow);
            }

            changeInProgress = false;
        }
        else if ("selectedRows".equals(evt.getPropertyName())) {
            changeInProgress = true;

            BaseChartPanel chartPanel = getChartPanel();

            if (chartPanel != null) {
                boolean[] selectedRows = (boolean[]) evt.getNewValue();
                chartPanel.clearHighlightedElements();

                for (int i = 0; i < selectedRows.length; i++) {
                    if (selectedRows[i]) {
                        int row = summaryTable.getDatasetRow(i);
                        int column = summaryTable.getDatasetColumn(i);

                        chartPanel.highlightElement(row, column);
                    }
                }
            }

            changeInProgress = false;
        }
        else if ("rowVisible".equals(evt.getPropertyName())) {
            changeInProgress = true;
            BaseChartPanel chartPanel = getChartPanel();
            Object[] values = (Object[]) evt.getNewValue();

            chartPanel.setElementVisible((Integer) values[0], (Integer) values[1], (Boolean) values[2]);

            changeInProgress = false;
        }
    }

    /**
     * @return the chart panel currently associated with this view; can be <code>null</code>
     */
    protected abstract BaseChartPanel getChartPanel();

    protected abstract void saveCharts(String directory);
}
