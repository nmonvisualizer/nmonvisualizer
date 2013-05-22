package com.ibm.nmon.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.text.SimpleDateFormat;

import com.ibm.nmon.gui.Styles;

public final class BasicFormatter extends Formatter {
    private final SimpleDateFormat format = new SimpleDateFormat(Styles.DATE_FORMAT_STRING_SHORT);

    @Override
    public String format(LogRecord record) {
        StringWriter buffer = new StringWriter(256);

        buffer.append(format.format(record.getMillis()));
        buffer.append(" ");
        buffer.append(record.getLevel().getName());
        buffer.append(" ");
        buffer.append(record.getSourceClassName());
        buffer.append(".");
        buffer.append(record.getSourceMethodName());
        buffer.append(": ");
        buffer.append(record.getMessage());
        buffer.append("\n");

        if (record.getThrown() != null) {
            PrintWriter pw = new PrintWriter(buffer);
            record.getThrown().printStackTrace(pw);

            pw.close();
        }
        // else nothing since StringWriter.close() is a no-op

        return buffer.toString();
    }
}
