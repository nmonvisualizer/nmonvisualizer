package com.ibm.nmon.interval;

import java.util.List;

import java.util.Set;

import com.ibm.nmon.util.TimeFormatCache;

/**
 * A manager implementation for {@link Interval intervals}. Maintains a list of multiple intervals
 * along with a 'current' interval that is being analyzed. On changes, the manager will fire
 * {@link IntervalListener} events.
 */
public final class IntervalManager {
    private final Set<Interval> intervals;

    private Interval currentInterval;

    private List<IntervalListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<IntervalListener>();

    public IntervalManager() {
        intervals = new java.util.TreeSet<Interval>();
        currentInterval = Interval.DEFAULT;
    }

    public Interval getCurrentInterval() {
        return currentInterval;
    }

    public void setCurrentInterval(Interval interval) {
        if (intervals.contains(interval)) {
            if (!currentInterval.equals(interval)) {
                currentInterval = interval;

                for (IntervalListener listener : listeners) {
                    listener.currentIntervalChanged(interval);
                }
            }
        }
        else if (Interval.DEFAULT.equals(interval)) {
            // call the listeners even if the current interval is already Interval.DEFAULT
            // this is needed in case data is added that redefines what the default interval time
            // span actually is
            // see com.ibm.nmon.NMONVisualizerApp.recalculateMinAndMaxSystemTime()
            currentInterval = Interval.DEFAULT;

            for (IntervalListener listener : listeners) {
                listener.currentIntervalChanged(interval);
            }
        }
    }

    public Iterable<Interval> getIntervals() {
        return java.util.Collections.unmodifiableSet(intervals);
    }

    public int getIntervalCount() {
        return intervals.size();
    }

    public boolean addInterval(Interval interval) {
        if (Interval.DEFAULT.equals(interval)) {
            return false;
        }
        else if (intervals.add(interval)) {
            for (IntervalListener listener : listeners) {
                listener.intervalAdded(interval);
            }

            return true;
        }
        else {
            return false;
        }
    }

    public boolean removeInterval(Interval interval) {
        if (intervals.remove(interval)) {
            for (IntervalListener listener : listeners) {
                listener.intervalRemoved(interval);
            }

            if (currentInterval.equals(interval)) {
                setCurrentInterval(Interval.DEFAULT);
            }

            return true;
        }
        else {
            return false;
        }
    }

    public void clearIntervals() {
        intervals.clear();

        for (IntervalListener listener : listeners) {
            listener.intervalsCleared();
        }

        setCurrentInterval(Interval.DEFAULT);
    }

    public void renameInterval(Interval interval, String newName) {
        if (intervals.contains(interval)) {
            if (newName == null) {
                newName = "";
            }

            if (!interval.getName().equals(newName)) {
                interval.setName(newName);
                TimeFormatCache.renameInterval(interval);

                for (IntervalListener listener : listeners) {
                    listener.intervalRenamed(interval);
                }
            }
        }
    }

    public void addListener(IntervalListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(IntervalListener listener) {
        listeners.remove(listener);
    }
}
