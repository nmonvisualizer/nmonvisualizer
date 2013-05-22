package com.ibm.nmon.interval;

/**
 * Interface defining events fired by {@link IntervalManager}.
 */
public interface IntervalListener {
    public void intervalAdded(Interval interval);

    public void intervalRemoved(Interval interval);

    public void intervalsCleared();

    public void currentIntervalChanged(Interval interval);

    public void intervalRenamed(Interval interval);
}
