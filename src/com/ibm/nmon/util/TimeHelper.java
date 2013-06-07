package com.ibm.nmon.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Utility methods for working with times.
 */
public final class TimeHelper {
    public static final SimpleDateFormat TIMESTAMP_FORMAT_ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ");
    public static final SimpleDateFormat DATE_FORMAT_ISO = new SimpleDateFormat("yyyy-MM-dd");

    public static long dayFromDatetime(long datetime) {
        Calendar cal = new java.util.GregorianCalendar();
        cal.setTimeInMillis(datetime);

        // today
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime().getTime();
    }

    public static long today() {
        return dayFromDatetime(System.currentTimeMillis());
    }

    private TimeHelper() {}
}
