package com.ibm.nmon.parser.gc.state;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.parser.gc.GCParserContext;

/**
 * The initial state for a Java 7-style garbage collection parser.
 */
final class Java7GC implements GCState {
    static final Java7GC INSTANCE = new Java7GC();

    private final SimpleDateFormat datetime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US);

    private final Initialized initialized = new Initialized(this);

    private double timeSince;
    private double afInterval;

    private int totalCount;
    private int nurseryCount;
    private int tenuredCount;
    private int systemCount;

    @Override
    public GCState startElement(GCParserContext context, String elementName, String unparsedAttributes) {
        if ("initialized".equals(elementName)) {
            return initialized;
        }
        else if ("exclusive-start".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            timeSince = context.parseDouble("intervalms") / 1000;

            context.setCurrentRecord(new DataRecord(parseTimestamp(context), String.format("%08x",
                    context.getData().getRecordCount()).toString()));
        }
        else if ("sys-start".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            context.setValue("GCSINCE", "gc_system", timeSince);
            context.setValue("GCCOUNT", "system_count", ++systemCount);
        }
        else if ("af-start".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            afInterval = context.parseDouble("intervalms") / 1000;

            context.setValue("GCMEM", "requested", "totalBytesRequested");
        }
        else if ("cycle-start".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            String type = context.getAttribute("type");

            if (type.startsWith("global")) { // global or 'global garbage collect'
                context.setValueDiv1000("GCSINCE", "gc_global", "intervalms");

                if (afInterval >= 0) {
                    context.setValue("GCSINCE", "af_tenured", afInterval);
                }

                context.setValue("GCCOUNT", "tenured_count", ++tenuredCount);
            }
            else if ("scavenge".equals(type)) {
                context.setValueDiv1000("GCSINCE", "gc_scavenger", "intervalms");

                if (afInterval >= 0) {
                    context.setValue("GCSINCE", "af_nursery", afInterval);
                }

                context.setValue("GCCOUNT", "nursery_count", ++nurseryCount);
            }

            context.setValue("GCCOUNT", "total_count", ++totalCount);

            return Java7GCCycle.INSTANCE;
        }
        else if ("response-info".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            context.setValue("GCTIME", "exclusive_ms", "timems");
        }
        else if ("exclusive-end".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            context.setValue("GCTIME", "total_ms", "durationms");
        }

        return this;
    }

    @Override
    public GCState endElement(GCParserContext context, String elementName) {
        if ("verbosegc".equals(elementName)) {
            context.reset();
            reset();

            return Start.INSTANCE;
        }
        else if ("exclusive-end".equals(elementName)) {
            context.saveRecord();

            timeSince = 0;
            afInterval = -1;

            Java7GCCycle.INSTANCE.reset();

            return this;
        }
        else {
            return this;
        }
    }

    @Override
    public void reset() {
        timeSince = 0;
        afInterval = -1;

        totalCount = 0;
        nurseryCount = 0;
        tenuredCount = 0;
        systemCount = 0;

        Java7GCCycle.INSTANCE.reset();
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

    private Java7GC() {
        reset();
    }

}
