package com.ibm.nmon.util;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.slf4j.Logger;

import java.io.StringWriter;
import java.io.PrintWriter;

import com.ibm.nmon.parser.NMONParser;

/**
 * <p>
 * Custom {@link java.util.logging.Handler Handler} that writes log messages to an internal buffer.
 * The buffer can be retrieved using the {@link #getMessages()} method.
 * </p>
 * 
 * <p>
 * To allow tracking by the application, parsers should call {@link #setCurrentFilename(String)}
 * when parsing a file.
 * </p>
 * 
 * <p>
 * This class is a singleton to ensure that only on instance is added to the Logger for
 * 'com.ibm.nmon.parser' package.
 * </p>
 */
public final class ParserLog extends Handler {
    private Logger logger;
    private String currentFilename;
    private StringWriter logBuffer;

    private boolean hasData;

    private ParserLog() {
        // note the parser package, not this class' package
        String loggerName = NMONParser.class.getPackage().getName();

        // configure underlying JDK logger ...
        java.util.logging.Logger parserLogger = java.util.logging.Logger.getLogger(loggerName);
        parserLogger.addHandler(this);

        // but expose SLF4J logger
        logger = org.slf4j.LoggerFactory.getLogger(loggerName);

        currentFilename = "";
        logBuffer = new StringWriter(512);
        hasData = false;
    }

    private static final ParserLog INSTANCE = new ParserLog();

    public static synchronized ParserLog getInstance() {
        return INSTANCE;
    }

    @Override
    public void close() {
        try {
            logBuffer.close();
        }
        catch (java.io.IOException ioe) {
            // ignore
        }
        finally {
            logBuffer = null;
        }
    }

    @Override
    public void flush() {}

    @Override
    public synchronized void publish(LogRecord record) {
        logBuffer.append(record.getLevel().getName());
        logBuffer.append("  ");
        logBuffer.append(record.getMessage());
        logBuffer.append("\n");

        if (record.getThrown() != null) {
            PrintWriter pw = new PrintWriter(logBuffer);
            record.getThrown().printStackTrace(pw);

            pw.close();
        }

        hasData = true;
    }

    public Logger getLogger() {
        return logger;
    }

    public synchronized boolean hasData() {
        return hasData;
    }

    public synchronized String getCurrentFilename() {
        return currentFilename;
    }

    public synchronized void setCurrentFilename(String currentFilename) {
        if (currentFilename == null) {
            this.currentFilename = "";
        }
        else {
            this.currentFilename = currentFilename;
        }
    }

    public synchronized String getMessages() {
        String toReturn = logBuffer.toString();
        close();

        currentFilename = "";
        logBuffer = new StringWriter(512);
        hasData = false;

        return toReturn;
    }
}
