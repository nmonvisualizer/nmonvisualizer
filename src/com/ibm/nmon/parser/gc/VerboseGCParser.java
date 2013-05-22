package com.ibm.nmon.parser.gc;

import java.io.IOException;
import java.io.File;

import java.util.TimeZone;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.BasicDataSet;

import com.ibm.nmon.parser.BasicXMLParser;

import com.ibm.nmon.parser.gc.state.GCState;
import com.ibm.nmon.parser.gc.state.Start;

/**
 * <p>
 * Parser for IBM JVM verbose GC files.
 * </p>
 * 
 * <p>
 * Note that verbose GC files do not include metadata about where the file was collected (JVM name
 * and hostname). As a result the parse methods require these values to be passed in. It is up to
 * the caller to get these values.
 * </p>
 */
public class VerboseGCParser extends BasicXMLParser {
    // this class delegates all parsing a state machine composed of GCState objects
    // the context is created on each call to parse()
    private GCParserContext context;
    private GCState currentState;

    public VerboseGCParser() {
        reset();
    }

    public DataSet parse(File file, TimeZone timeZone, String hostname, String jvmName) throws IOException {
        return parse(file.getAbsolutePath(), timeZone, hostname, jvmName);
    }

    public DataSet parse(String filename, TimeZone timeZone, String hostname, String jvmName) throws IOException {
        long start = System.nanoTime();

        BasicDataSet data = new BasicDataSet(filename);
        data.setHostname(hostname);

        data.setMetadata("jvm_name", jvmName);

        context = new GCParserContext(data, logger);

        try {
            parse(filename);

            if (logger.isDebugEnabled()) {
                logger.debug("Parse complete for file '{}' in {}ms", data.getSourceFile(),
                        (System.nanoTime() - start) / 1000000.0d);
            }

            if (data.getRecordCount() == 0) {
                throw new IOException("verbose GC log file '" + filename + "' does not appear to have any data records");
            }

            return data;
        }
        finally {
            reset();
        }
    }

    protected void reset() {
        super.reset();
        context = null;

        currentState = Start.INSTANCE;
        currentState.reset();
    }

    public void startElement(String element, String unparsedAttributes) {
        context.setLineNumber(getLineNumber());
        currentState = currentState.startElement(context, element, unparsedAttributes);
    }

    public void endElement(String element) {
        context.setLineNumber(getLineNumber());
        currentState = currentState.endElement(context, element);
    }
}
