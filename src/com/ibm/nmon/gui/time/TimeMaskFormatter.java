package com.ibm.nmon.gui.time;

import java.text.ParseException;
import java.util.regex.Pattern;

import javax.swing.text.MaskFormatter;

import javax.swing.text.DefaultFormatterFactory;

/**
 * MaskFormatter for times in 24 hour format. This formatter ensures only valid times are entered.
 */
public final class TimeMaskFormatter extends MaskFormatter {
    private static final long serialVersionUID = -7862295130660227255L;

    private static final TimeMaskFormatter START = new TimeMaskFormatter("00:00:00");
    private static final TimeMaskFormatter END = new TimeMaskFormatter("23:59:59");

    private static final Pattern TIME_REGEX = Pattern.compile("^(([0-1]?[0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]$");

    public static DefaultFormatterFactory createFormatterFactory(boolean start) {
        // START and END may need to be cloned if used in multiple threads
        if (start) {
            return new DefaultFormatterFactory(START, START, START, START);
        }
        else {
            return new DefaultFormatterFactory(END, END, END, END);
        }
    }

    private TimeMaskFormatter(String defaultValue) {
        super();

        try {
            setMask("##:##:##");
            setPlaceholder(defaultValue);
            setPlaceholderCharacter('_');
            setValueClass(Void.class);
        }
        catch (ParseException pe) {
            // should never happen since the regex is hardcoded
            pe.printStackTrace();
        }
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return getPlaceholder();
        }
        else {
            String time = formatTime((Integer) value);
            // System.out.println("vts " + value + "="+ time);

            return time;
        }
    }

    public Object stringToValue(String value) throws ParseException {
        if (TIME_REGEX.matcher(value).matches()) {
            return parseTime(value);
        }
        else {
            throw new ParseException("invalid time", 0);
        }
    }

    /*
     * public static boolean isValidTime(String time) { return TIME_REGEX.matcher(time).matches(); }
     */
    public static int parseTime(String time) {
        int hours = Integer.parseInt(time.substring(0, 2));
        int minutes = Integer.parseInt(time.substring(3, 5));
        int seconds = Integer.parseInt(time.substring(6, 8));

        return (hours * 3600 + minutes * 60 + seconds);
    }

    private static String formatTime(int time) {
        int hours = (time % 86400) / 3600;
        int minutes = (time % 3600) / 60;
        int seconds = (time % 60);

        StringBuilder builder = new StringBuilder(8);

        if (hours < 10) {
            builder.append('0');
        }

        builder.append(hours);
        builder.append(':');

        if (minutes < 10) {
            builder.append('0');
        }

        builder.append(minutes);
        builder.append(':');

        if (seconds < 10) {
            builder.append('0');
        }

        builder.append(seconds);

        // System.out.println(time + "=" + builder.toString());
        return builder.toString();
    }
}
