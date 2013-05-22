package com.ibm.nmon.util;

import java.util.List;

import java.util.TimeZone;
import java.util.SimpleTimeZone;

/**
 * Creates a list of TimeZones for use in the UI. The current implementation just creates a
 * {@link SimpleTimeZone} for all 25 hours from -12 UTC to +12 UTC.
 */
public final class TimeZoneFactory {
    public static final List<TimeZone> TIMEZONES = new java.util.ArrayList<TimeZone>(25);

    static {
        for (int i = -12; i <= 12; i++) {
            String id = "UTC";

            if (i < 0) {
                id += i;
                id += ":00";
            }
            else if (i > 0) {
                id += '+';
                id += i;
                id += ":00";
            }

            TIMEZONES.add(new SimpleTimeZone(i * 3600000, id));
        }
    }

    private TimeZoneFactory() {}
}
