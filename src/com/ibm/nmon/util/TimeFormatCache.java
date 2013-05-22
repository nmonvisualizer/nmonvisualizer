package com.ibm.nmon.util;

import java.text.SimpleDateFormat;

import java.util.Map;
import java.util.LinkedHashMap;

import java.util.TimeZone;

import org.slf4j.Logger;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.interval.Interval;

/**
 * <p>
 * Helper cache class for storing formatted time and interval strings. This class should be used
 * when displaying such Strings in the UI rather than recreating them each time they are displayed.
 * </p>
 * 
 * <p>
 * This classes uses an LRU caching strategy to keep the number of cached strings small. The UI is
 * responsible for updating this cache when the time zone, and thus the format of the strings,
 * changes - see {@link #setTimeZone(TimeZone) setTimeZone()}.
 * </p>
 */
public final class TimeFormatCache {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TimeFormatCache.class);

    // assume all access to these fields is in the Swing thread, so no need to synchronize
    private static final Map<Interval, String> FORMATTED_INTERVALS = new LRUMap<Interval, String>(25);
    private static final Map<Long, String> FORMATTED_DATETIMES = new LRUMap<Long, String>(100);
    private static final Map<Long, String> FORMATTED_TIMES = new LRUMap<Long, String>(100);

    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat(Styles.DATE_FORMAT_STRING);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(Styles.DATE_FORMAT_STRING_SHORT);

    private static long DEFAULT_INTERVAL_MIN;
    private static long DEFAULT_INTERVAL_MAX;

    public static String formatInterval(Interval interval) {
        String formattedInterval = FORMATTED_INTERVALS.get(interval);

        if (formattedInterval == null) {
            if (Interval.DEFAULT.equals(interval)) {
                if ((DEFAULT_INTERVAL_MIN > 0) && (DEFAULT_INTERVAL_MAX < Long.MAX_VALUE)) {
                    formattedInterval = "All Data" + ": " + formatDateTime(DEFAULT_INTERVAL_MIN) + " - "
                            + formatDateTime(DEFAULT_INTERVAL_MAX);

                }
                else {
                    formattedInterval = "All Data";
                }
            }
            else {
                formattedInterval = formatDateTime(interval.getStart()) + " - " + formatDateTime(interval.getEnd());

                if (!"".equals(interval.getName())) {
                    String name = interval.getName();

                    if (name.length() > 25) {
                        name = name.substring(0, 22);
                        name += "...";
                    }

                    formattedInterval = name + ": " + formattedInterval;
                }
            }

            FORMATTED_INTERVALS.put(interval, formattedInterval);
        }

        return formattedInterval;
    }

    public static void setDefaultIntervalRange(long minTime, long maxTime) {
        TimeFormatCache.DEFAULT_INTERVAL_MIN = minTime;
        TimeFormatCache.DEFAULT_INTERVAL_MAX = maxTime;

        FORMATTED_INTERVALS.remove(Interval.DEFAULT);
    }

    public static void renameInterval(Interval i) {
        FORMATTED_INTERVALS.remove(i);
    }

    public static String formatDateTime(long data) {
        String formattedTime = FORMATTED_DATETIMES.get(data);

        if (formattedTime == null) {
            formattedTime = DATETIME_FORMAT.format(data);
            FORMATTED_DATETIMES.put(data, formattedTime);
        }

        return formattedTime;
    }

    public static String formatTime(long data) {
        String formattedTime = FORMATTED_TIMES.get(data);

        if (formattedTime == null) {
            formattedTime = TIME_FORMAT.format(data);
            FORMATTED_TIMES.put(data, formattedTime);
        }

        return formattedTime;
    }

    public static void setTimeZone(TimeZone timeZone) {
        DATETIME_FORMAT.setTimeZone(timeZone);
        TIME_FORMAT.setTimeZone(timeZone);

        FORMATTED_INTERVALS.clear();
        FORMATTED_DATETIMES.clear();
        FORMATTED_TIMES.clear();
    }

    private TimeFormatCache() {}

    private static final class LRUMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = -1440114072711805032L;

        private final int maxSize;

        LRUMap(int maxSize) {
            this.maxSize = maxSize;
        }

        public V put(K key, V value) {
            V v = super.put(key, value);

            if (v == null) {
                TimeFormatCache.LOGGER.trace("cached {}={}", key, value);
            }

            return v;
        };

        protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
            boolean oversized = size() > maxSize;

            if (oversized) {
                TimeFormatCache.LOGGER.trace("evicted {}={}", eldest.getKey(), eldest.getValue());
            }

            return oversized;
        };
    }
}
