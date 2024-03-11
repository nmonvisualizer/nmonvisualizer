package com.ibm.nmon.util;

import java.util.List;

import java.util.TimeZone;
import java.util.SimpleTimeZone;

/**
 * Creates a list of TimeZones for use in the UI. Create a
 * {@link SimpleTimeZone} for all well known time zones.
 */
public final class TimeZoneFactory {
    public static final List<TimeZone> TIMEZONES;
    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int MILLISECONDS_IN_SECOND = 1000;

    static {
        List<TimeZone> temp = new java.util.ArrayList<TimeZone>(25);

        for (int i = -12; i <= 14; i++) {
        	temp.add(createTimeZone(i, 0));
        	// https://en.wikipedia.org/wiki/List_of_UTC_offsets
        	if (i == -9 || i == -3 || i == 3 || i == 4 || i == 6 || i == 9 || i == 10) {
        		temp.add(createTimeZone(i, 30));
        	} else if (i == 5) {
        		temp.add(createTimeZone(i, 30));
        		temp.add(createTimeZone(i, 45));
        	} else if (i == 8 || i == 12) {
        		temp.add(createTimeZone(i, 45));
        	}
        }

        TIMEZONES = java.util.Collections.unmodifiableList(temp);
    }

	private static SimpleTimeZone createTimeZone(int i, int extraOffsetMinutes) {
		String id = "UTC";

		int rawOffset = i * SECONDS_IN_MINUTE * MINUTES_IN_HOUR * MILLISECONDS_IN_SECOND;

		if (i != 0) {
			if (i > 0) {
			    id += '+';
			}
	
			id += i;
			
			if (extraOffsetMinutes == 0) {
			    id += ":00";
			} else {
			    id += ":" + extraOffsetMinutes;
			    extraOffsetMinutes *= SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND;
			    if (i > 0) {
			    	rawOffset += extraOffsetMinutes;
			    } else {
			    	rawOffset += -1 * extraOffsetMinutes;
			    }
			}
		}

		return new SimpleTimeZone(rawOffset, id);
	}

    private TimeZoneFactory() {}
}
