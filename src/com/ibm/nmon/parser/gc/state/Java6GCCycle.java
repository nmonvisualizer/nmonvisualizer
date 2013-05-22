package com.ibm.nmon.parser.gc.state;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.parser.gc.GCParserContext;

/**
 * Base class for Java 6 style verbose GC cycles.
 */
abstract class Java6GCCycle extends JavaGCCycle {
    private final SimpleDateFormat datetime = new SimpleDateFormat("MMM dd HH:mm:ss yyyy", java.util.Locale.US);

    // the XML element name for the GC cycle
    protected final String transitionElement;
    // the DataType field for the GC cycle
    private final String intervalField;

    // rather than parse the timestamp for each cycle, which is accurate only to the second,
    // parse the interval ms, which is microseconds accurate, and keep an accumulated total
    // from the first timestamp seen in the file for each gc type
    private long baseTime;
    private double accumulatedTime;

    // state for handling <gc> elements
    private final Java6Collection collection;

    Java6GCCycle(String transitionElement, String intervalField) {
        super();

        this.transitionElement = transitionElement;
        this.intervalField = intervalField;

        this.collection = new Java6Collection(this);
    }

    @Override
    public final GCState startElement(GCParserContext context, String elementName, String unparsedAttributes) {
        if (transitionElement.equals(elementName)) {
            // Java6GC will have already called context.parseAttributes()
            return onStartCycle(context, elementName, unparsedAttributes);
        }
        else if ("gc".equals(elementName)) {
            beforeGC = false;
            return collection.startElement(context, elementName, unparsedAttributes);
        }
        else if ("time".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            if (beforeGC) {
                double exclusive = context.parseDouble("exclusiveaccessms");

                if (exclusive > (86400 * 1000)) {
                    context.logInvalidValue("exlusiveaccessms", context.getAttribute("exclusiveaccessms"));

                    error = true;
                }
                else {
                    context.setValue("GCTIME", "exclusive_ms", exclusive);
                }
            }
            else {
                context.setValue("GCTIME", "total_ms", "totalms");
            }
        }
        else if ("nursery".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            calculateSizes(context, elementName, "freebytes", "totalbytes");
        }
        else if ("tenured".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            calculateSizes(context, elementName, "freebytes", "totalbytes");
        }
        else if ("refs_cleared".equals(elementName) || "refs".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            context.setValue("GCSTAT", "soft", "soft");
            context.setValue("GCSTAT", "weak", "weak");
            context.setValue("GCSTAT", "phantom", "phantom");
        }
        else if ("minimum".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            context.setValue("GCMEM", "requested", "requested_bytes");
        }

        return this;
    }

    @Override
    public GCState endElement(GCParserContext context, String elementName) {
        if (elementName.equals(transitionElement)) {
            if (!error) {
                calculateTotalSizes(context);
                context.saveRecord();
            }

            error = false;
            beforeGC = true;

            return Java6GC.INSTANCE;
        }
        else {
            return this;
        }
    }

    @Override
    public void reset() {
        super.reset();

        this.baseTime = 0;
        this.accumulatedTime = 0;
    }

    protected GCState onStartCycle(GCParserContext context, String elementName, String unparsedAttributes) {
        calculateTime(context);
        return this;
    }

    protected final void calculateTime(GCParserContext context) {
        boolean first = false;

        if (baseTime == 0) {
            baseTime = parseTimestamp(context);

            if (baseTime == 0) {
                error = true;
                return;
            }

            first = true;
        }

        // use the interval milliseconds to calculate the actual timestamp for the DataRecord
        double intervalms = context.parseDouble("intervalms");

        if (Double.isNaN(intervalms)) {
            error = true;
            return;
        }

        // sanity check for bugs seen in AIX verbose GC logs with incorrectly huge intervals
        // assume it is nearly impossible to not run GC once a day
        if (intervalms > (86400 * 1000)) {
            context.logInvalidValue("intervalms", context.getAttribute("intervalms"));

            error = true;
            return;
        }

        accumulatedTime += intervalms;

        context.setCurrentRecord(new DataRecord((long) (baseTime + accumulatedTime), String.format("%08x",
                context.getData().getRecordCount()).toString()));

        if (!first) {
            // do not record a zero for the first interval
            context.setValue("GCSINCE", intervalField, intervalms / 1000);
        }
    }

    private long parseTimestamp(GCParserContext context) {
        String value = context.getAttribute("timestamp");

        if (value == null) {
            context.logMissingAttribute("timestamp");
            return 0;
        }

        long toReturn;

        try {
            toReturn = datetime.parse(value).getTime();
        }
        catch (ParseException pe) {
            context.logInvalidValue("timestamp", value);

            toReturn = 0;
        }

        return toReturn;
    }
}
