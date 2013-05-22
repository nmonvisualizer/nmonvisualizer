package com.ibm.nmon.gui.analysis;

import java.util.BitSet;
import java.util.List;

import com.ibm.nmon.analysis.AnalysisSet;

import com.ibm.nmon.analysis.Statistic;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Table model that displays a selectable metric for each measurement in an AnalysisSet. Data for
 * all data sets is displayed; each data set is a separate column.
 */
public final class ByDataSetTableModel extends AnalysisSetTableModel {
    private Statistic stat;

    private final List<String> columns;

    public ByDataSetTableModel(NMONVisualizerGui gui, AnalysisSet analysisSet) {
        super(gui, analysisSet);

        columns = new java.util.ArrayList<String>(2 + gui.getDataSetCount());

        rebuildColumns();
        buildColumnNameMap();

        stat = Statistic.AVERAGE;

        // alert the owning table that it has columns so it can be sized in the column model
        fireTableStructureChanged();
    }

    @Override
    public String[] getAllColumns() {
        return columns.toArray(new String[columns.size()]);
    }

    @Override
    public boolean getDefaultColumnState(int column) {
        return true;
    }

    @Override
    public boolean canDisableColumn(int column) {
        if (column < 2) {
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    protected Class<?> getEnabledColumnClass(int columnIndex) {
        if (columnIndex < 2) {
            return String.class;
        }
        else {
            if (Statistic.COUNT == stat) {
                return Integer.class;
            }
            else {
                return Double.class;
            }
        }
    }

    @Override
    protected String getEnabledColumnName(int column) {
        return columns.get(column);
    }

    @Override
    protected Object getEnabledValueAt(int rowIndex, int columnIndex) {
        String key = keys.get(rowIndex);

        if (key == null) {
            throw new ArrayIndexOutOfBoundsException(rowIndex);
        }

        if (columnIndex == 0) {
            return analysisSet.getType(key);
        }
        else if (columnIndex == 1) {
            return analysisSet.getField(key);
        }
        else {
            int n = 2;

            DataSet data = null;

            for (DataSet toTest : gui.getDataSets()) {
                if (n++ == columnIndex) {
                    data = toTest;
                    break;
                }
            }

            if (data == null) {
                throw new ArrayIndexOutOfBoundsException(columnIndex);
            }

            return stat.getValue(gui.getAnalysis(data), analysisSet.getType(key), analysisSet.getField(key));
        }
    }

    public void setStatistic(Statistic stat) {
        this.stat = stat;
        fireTableDataChanged();
    }

    @Override
    public void dataAdded(DataSet data) {
        // adding columns => structure change
        rebuildColumns();
    }

    @Override
    public void dataRemoved(DataSet data) {
        rebuildColumns();
    }

    @Override
    public void dataCleared() {
        super.dataCleared();
        rebuildColumns();
    }

    private void rebuildColumns() {
        columns.clear();

        columns.add("Data Type");
        columns.add("Metric");

        for (DataSet data : gui.getDataSets()) {
            columns.add(data.getHostname());
        }

        BitSet oldEnabled = enabledColumns;

        enabledColumns = new BitSet(columns.size());

        if (oldEnabled != null) {
            int stop = Math.min(oldEnabled.length(), columns.size());

            for (int i = 0; i < stop; i++) {
                enabledColumns.set(i, oldEnabled.get(i));
            }

            // show just added data, if any
            enabledColumns.set(columns.size() - 1, true);
        }
        else {
            // null => just created, show everything
            enabledColumns.set(0, columns.size(), true);
        }

        buildColumnNameMap();

        fireTableStructureChanged();
    }
}
