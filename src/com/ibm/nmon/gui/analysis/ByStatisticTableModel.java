package com.ibm.nmon.gui.analysis;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.BitSet;

import com.ibm.nmon.analysis.AnalysisSet;
import com.ibm.nmon.analysis.Statistic;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Table model that displays min, max, average and standard deviation for each measurement in an
 * AnalysisSet. Data for all data sets is displayed, one row per file / measurement combination.
 */
public final class ByStatisticTableModel extends AnalysisSetTableModel implements PropertyChangeListener {
    // last null is for GRANULARITY_MAXIMUM which will have a custom name
    private static final String[] COLUMN_NAMES = new String[] { "Hostname", "Data Type", "Metric",
            Statistic.MINIMUM.toString(), Statistic.AVERAGE.toString(), Statistic.MAXIMUM.toString(),
            Statistic.STD_DEV.toString(), Statistic.MEDIAN.toString(), Statistic.SUM.toString(),
            Statistic.COUNT.toString(), null };

    private static final boolean[] DEFAULT_COLUMNS = new boolean[] { true, true, true, true, true, true, true, false,
            false, false, false };

    static {
        if (COLUMN_NAMES.length != DEFAULT_COLUMNS.length) {
            throw new IllegalArgumentException("default values array size not equal to column names size "
                    + COLUMN_NAMES.length + " != " + DEFAULT_COLUMNS.length);
        }
    }

    public ByStatisticTableModel(NMONVisualizerGui gui, AnalysisSet analysisSet) {
        super(gui, analysisSet);

        COLUMN_NAMES[COLUMN_NAMES.length - 1] = Statistic.GRANULARITY_MAXIMUM.getName(gui.getGranularity());
        buildColumnNameMap();

        enabledColumns = new BitSet(COLUMN_NAMES.length);

        for (int i = 0; i < DEFAULT_COLUMNS.length; i++) {
            enabledColumns.set(i, DEFAULT_COLUMNS[i]);
        }

        // alert the owning table that it has columns so it can be sized in the column model
        fireTableStructureChanged();

        gui.addPropertyChangeListener("granularity", this);
    }

    @Override
    public String getKey(int index) {
        int keyIndex = index / gui.getDataSetCount();
        return keys.get(keyIndex);
    }

    @Override
    public int getRowCount() {
        // each file displays a measurement for every key
        return keys.size() * gui.getDataSetCount();
    }

    @Override
    public String[] getAllColumns() {
        return COLUMN_NAMES;
    }

    @Override
    public boolean getDefaultColumnState(int column) {
        return DEFAULT_COLUMNS[column];
    }

    @Override
    public boolean canDisableColumn(int column) {
        if (column < 3) {
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    protected Class<?> getEnabledColumnClass(int columnIndex) {
        if (columnIndex < 3) {
            return String.class;
        }
        else if (columnIndex == 9) {
            return Integer.class;
        }
        else {
            return Double.class;
        }
    }

    @Override
    protected String getEnabledColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    protected Object getEnabledValueAt(int rowIndex, int columnIndex) {
        // display table as
        // file 1, key 1
        // file 2, key 1
        // ...
        // file 1, key 1
        // file 2, key 2
        // ...
        // file n, key n

        // need to map key rows from base model to actual rows
        int fileIndex = rowIndex % gui.getDataSetCount();
        DataSet data = null;
        int n = 0;

        for (DataSet toSearch : gui.getDataSets()) {
            if (n++ == fileIndex) {
                data = toSearch;
                break;
            }
        }

        if (data == null) {
            throw new ArrayIndexOutOfBoundsException(rowIndex);
        }

        int keyIndex = rowIndex / gui.getDataSetCount();
        String key = keys.get(keyIndex);

        switch (columnIndex) {
        case 0:
            return data.toString();
        case 1:
            return analysisSet.getType(key);
        case 2:
            return analysisSet.getField(key);
        case 3:
            return gui.getAnalysis(data).getMinimum(analysisSet.getType(key), analysisSet.getField(key));
        case 4:
            return gui.getAnalysis(data).getAverage(analysisSet.getType(key), analysisSet.getField(key));
        case 5:
            return gui.getAnalysis(data).getMaximum(analysisSet.getType(key), analysisSet.getField(key));
        case 6:
            return gui.getAnalysis(data).getStandardDeviation(analysisSet.getType(key), analysisSet.getField(key));
        case 7:
            return gui.getAnalysis(data).getMedian(analysisSet.getType(key), analysisSet.getField(key));
        case 8:
            return gui.getAnalysis(data).getSum(analysisSet.getType(key), analysisSet.getField(key));
        case 9:
            return gui.getAnalysis(data).getCount(analysisSet.getType(key), analysisSet.getField(key));
        case 10:
            return gui.getAnalysis(data).getGranularityMaximum(analysisSet.getType(key), analysisSet.getField(key));
        default:
            return new ArrayIndexOutOfBoundsException(columnIndex);
        }
    }

    // base table model fires updates on key changes
    // this model needs to update a row for each file
    @Override
    public void fireTableRowsInserted(int firstRow, int lastRow) {
        for (int i = firstRow; i <= lastRow; i++) {
            for (int j = 0; j < gui.getDataSetCount(); j++) {
                int actualRow = i * gui.getDataSetCount() + j;
                super.fireTableRowsInserted(actualRow, actualRow);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("granularity".equals(evt.getPropertyName())) {
            COLUMN_NAMES[COLUMN_NAMES.length - 1] = Statistic.GRANULARITY_MAXIMUM.getName(gui.getGranularity());
            buildColumnNameMap();

            if (enabledColumns.get(COLUMN_NAMES.length - 1)) {
                // update granularity max column name
                fireTableStructureChanged();
            }
        }
    }
}
