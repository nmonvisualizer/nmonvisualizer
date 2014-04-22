package com.ibm.nmon.gui.interval;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.interval.IntervalListener;
import com.ibm.nmon.interval.Interval;

/**
 * <p>
 * Table model for displaying a list of Intervals. Intervals are added and removed only by listening
 * to the IntervalManager.
 * </p>
 * 
 * <p>
 * This model maintains the intervals in sorted order.
 * </p>
 */
final class IntervalTableModel extends AbstractTableModel implements IntervalListener {
    private static final long serialVersionUID = 8444774364976685957L;

    private static final String[] columnNames = { "Name", "Start", "End" };

    private final List<Interval> intervals = new java.util.LinkedList<Interval>();

    IntervalTableModel(NMONVisualizerGui gui) {
        // potential race condition here in that intervals could be added between construction and
        // the time this model is added as a listener
        // not worrying about this under the assumption that the user cannot add intervals when then
        // gui is still being built
        for (Interval interval : gui.getIntervalManager().getIntervals()) {
            intervals.add(interval);
        }

        java.util.Collections.sort(intervals);
    }

    @Override
    public int getRowCount() {
        return intervals.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Class<?> getColumnClass(int c) {
        if (c == 0) {
            return String.class;
        }
        else {
            return Long.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return intervals.get(rowIndex).getName();
        case 1:
            return intervals.get(rowIndex).getStart();
        case 2:
            return intervals.get(rowIndex).getEnd();
        default:
            throw new IndexOutOfBoundsException();
        }
    }

    public Interval getValueAt(int rowIndex) {
        return intervals.get(rowIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }

    @Override
    public void intervalAdded(Interval interval) {
        for (int i = 0; i < intervals.size(); i++) {
            if (intervals.get(i).compareTo(interval) >= 0) {
                intervals.add(i, interval);

                fireTableRowsInserted(i, i);
                return;
            }
        }

        // have not added yet, just add at the end
        intervals.add(interval);

        fireTableRowsInserted(intervals.size() - 1, intervals.size() - 1);
    }

    @Override
    public void intervalRemoved(Interval interval) {
        for (int i = 0; i < intervals.size(); i++) {
            if (intervals.get(i).equals(interval)) {
                intervals.remove(i);

                fireTableRowsDeleted(i, i);
                return;
            }
        }
    }

    @Override
    public void intervalsCleared() {
        intervals.clear();

        fireTableDataChanged();
    }

    @Override
    public void currentIntervalChanged(Interval interval) {}

    @Override
    public void intervalRenamed(Interval interval) {
        for (int i = 0; i < intervals.size(); i++) {
            if (intervals.get(i).equals(interval)) {
                fireTableRowsUpdated(i, i);

                return;
            }
        }
    }
}
