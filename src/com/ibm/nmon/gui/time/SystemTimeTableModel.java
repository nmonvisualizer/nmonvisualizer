package com.ibm.nmon.gui.time;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.interval.Interval;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataSetListener;

/**
 * Displays a list of start and end times for all DataSets in the UI.
 */
public final class SystemTimeTableModel extends AbstractTableModel implements DataSetListener {
    private final NMONVisualizerGui gui;

    private Interval countInterval;
    private List<Integer> counts;

    public SystemTimeTableModel(NMONVisualizerGui gui) {
        this.gui = gui;
        countInterval = gui.getIntervalManager().getCurrentInterval();
        updateCounts();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return String.class;
        case 1:
            return Long.class;
        case 2:
            return Long.class;
        case 3:
            return Integer.class;
        default:
            throw new ArrayIndexOutOfBoundsException(columnIndex);
        }
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
        case 0:
            return "System";
        case 1:
            return "Data Start";
        case 2:
            return "Data End";
        case 3:
            return "Record Count";
        default:
            throw new ArrayIndexOutOfBoundsException(column);
        }
    }

    @Override
    public int getRowCount() {
        return gui.getDataSetCount();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DataSet data = null;

        int n = 0;

        for (DataSet toSearch : gui.getDataSets()) {
            if (n++ == rowIndex) {
                data = toSearch;
                break;
            }
        }

        if (data == null) {
            throw new ArrayIndexOutOfBoundsException(rowIndex);
        }

        switch (columnIndex) {
        case 0:
            return data.getHostname();
        case 1:
            return data.getStartTime();
        case 2:
            return data.getEndTime();
        case 3: {
            return counts.get(rowIndex);
        }
        default:
            throw new ArrayIndexOutOfBoundsException(columnIndex);
        }
    }

    public DataSet getValueAt(int rowIndex) {
        int n = 0;

        for (DataSet toSearch : gui.getDataSets()) {
            if (n++ == rowIndex) {
                return toSearch;
            }
        }

        throw new ArrayIndexOutOfBoundsException(rowIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void dataAdded(DataSet data) {
        updateCounts();
        fireTableDataChanged();
    }

    public void dataRemoved(DataSet data) {
        updateCounts();
        fireTableDataChanged();
    }

    @Override
    public void dataChanged(DataSet data) {
        updateCounts();
        fireTableDataChanged();
    }

    public void dataCleared() {
        updateCounts();
        fireTableDataChanged();
    }

    public Interval getCountInterval() {
        return countInterval;
    }

    public void setCountInterval(Interval countInterval) {
        if (!this.countInterval.equals(countInterval)) {
            this.countInterval = countInterval;
            updateCounts();

            fireTableDataChanged();
        }
    }

    // update dataset record counts when the interval changes
    // these are not cached under the assumption that this is a fast operation and datasets are
    // relatively small
    private void updateCounts() {
        counts = new java.util.ArrayList<Integer>(gui.getDataSetCount());

        for (DataSet data : gui.getDataSets()) {
            if (Interval.DEFAULT.equals(countInterval)) {
                counts.add(data.getRecordCount());
            }
            else {
                counts.add(data.getRecordCount(countInterval));
            }
        }
    }
}
