package com.ibm.nmon.util;

import com.ibm.nmon.NMONVisualizerApp;

import com.ibm.nmon.interval.Interval;

public class GranularityHelper {
    public static int DEFAULT_GRANULARITY = 60000;

    private final NMONVisualizerApp app;

    private int granularity = DEFAULT_GRANULARITY;
    private boolean automatic = false;

    public GranularityHelper(NMONVisualizerApp app) {
        this.app = app;
    }

    public int getGranularity() {
        return granularity;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        if (this.automatic != automatic) {
            this.automatic = automatic;

            if (this.automatic) {
                recalculate();
            }
        }
    }

    public void setGranularity(int granularity) {
        if (granularity < 0) {
            throw new IllegalArgumentException("granularity must be positive");
        }
        else {
            this.granularity = granularity;
            this.automatic = false;
        }
    }

    // attempt to get about 100 data points on each chart, based on the interval duration rounded to
    // the nearest 15 seconds
    // default to 60s
    public void recalculate() {
        if (!automatic) {
            throw new IllegalStateException("cannot automatically set granularity; call setAutomatic(true) first");
        }

        long duration = 0;

        Interval interval = app.getIntervalManager().getCurrentInterval();

        if (Interval.DEFAULT.equals(interval)) {
            duration = app.getMaxSystemTime() - app.getMinSystemTime();

            // no files parsed yet => default back to 60s
            if (duration == Long.MAX_VALUE) {
                granularity = 60000;
                return;
            }
        }
        else {
            duration = interval.getDuration();
        }

        long granularity = duration / 100;

        if (granularity >= Integer.MAX_VALUE) {
            granularity = 2147475000; // MAX_VALUE rounded down to nearest 15000
        }

        // round to nearest 15 seconds
        granularity = Math.round(granularity / 15000d) * 15000;

        if (granularity < 1000) {
            granularity = 1000;
        }

        this.granularity = (int) granularity;
    }
}
