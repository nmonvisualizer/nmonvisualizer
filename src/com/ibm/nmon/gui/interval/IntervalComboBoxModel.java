package com.ibm.nmon.gui.interval;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.interval.IntervalListener;
import com.ibm.nmon.interval.IntervalManager;

/**
 * <p>
 * Model for selecting from a list of Intervals. Intervals are added and removed only by listening
 * to the IntervalManager.
 * </p>
 * 
 * <p>
 * This model maintains the intervals in sorted order.
 * </p>
 */
final class IntervalComboBoxModel extends AbstractListModel<Interval> implements ComboBoxModel<Interval>,
        PropertyChangeListener, IntervalListener {
    private final IntervalManager manager;

    private final List<Interval> intervals = new java.util.LinkedList<Interval>();
    private Interval selected;

    public IntervalComboBoxModel(IntervalManager manager) {
        this.manager = manager;

        // potential race condition here in that intervals could be added between construction and
        // the time this model is added as a listener
        // not worrying about this under the assumption that the user cannot add intervals when then
        // gui is still being built
        for (Interval interval : manager.getIntervals()) {
            intervals.add(interval);
        }

        java.util.Collections.sort(intervals);

        selected = manager.getCurrentInterval();

        manager.addListener(this);
    }

    @Override
    public Interval getElementAt(int index) {
        if (index == 0) {
            return Interval.DEFAULT;
        }
        else {
            return intervals.get(index - 1);
        }
    }

    @Override
    public int getSize() {
        return intervals.size() + 1;
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (selectionChanged(anItem)) {
            manager.setCurrentInterval(selected);
        }
    }

    @Override
    public void intervalAdded(Interval interval) {
        for (int i = 0; i < intervals.size(); i++) {
            if (intervals.get(i).compareTo(interval) >= 0) {
                intervals.add(i, interval);

                // + 1 to count for DEFAULT
                fireIntervalAdded(this, i + 1, i + 1);
                return;
            }
        }

        // have not added yet, just add at the end
        intervals.add(interval);

        fireIntervalAdded(this, intervals.size(), intervals.size());

    }

    @Override
    public void intervalRemoved(Interval interval) {
        for (int i = 0; i < intervals.size(); i++) {
            if (intervals.get(i).equals(interval)) {
                intervals.remove(i);

                // + 1 to count for DEFAULT
                fireIntervalRemoved(this, i + 1, i + 1);

                return;
            }
        }
    }

    @Override
    public void intervalsCleared() {
        intervals.clear();
        selected = Interval.DEFAULT;

        fireContentsChanged(this, 1, Integer.MAX_VALUE);
    }

    @Override
    public void currentIntervalChanged(Interval interval) {
        if (selectionChanged(interval)) {
            fireContentsChanged(this, 0, 0);
        }
    }

    @Override
    public void intervalRenamed(Interval interval) {
        for (int i = 0; i < intervals.size(); i++) {
            if (intervals.get(i).equals(interval)) {
                // + 1 to count for DEFAULT
                fireContentsChanged(this, i + 1, i + 1);

                return;
            }
        }
    }

    private boolean selectionChanged(Object anItem) {
        // avoid infinite loops in that setSelectedItem needs to call currentIntervalChanged to
        // alert other listeners but currentIntervalChanged also needs to update the selected item
        // if the event is coming from another component
        if (selected.equals(anItem)) {
            return false;
        }
        else if (intervals.contains(anItem)) {
            selected = (Interval) anItem;

            return true;
        }
        else if (Interval.DEFAULT.equals(anItem)) {
            selected = Interval.DEFAULT;

            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        fireContentsChanged(this, 0, getSize() - 1);
    }
}
