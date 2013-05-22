package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.parser.gc.GCParserContext;

/**
 * The initial state for a Java 6 (and earlier) garbage collection parser. This state transitions to
 * {@link Java6GCCycle} States based on the current GC type encountered.
 */
final class Java6GC implements GCState {
    static final Java6GC INSTANCE = new Java6GC();

    private final Initialized initialized = new Initialized(this);

    // various GC cycle types
    private final Java6GCCycle nurseryAF = new Java6GCCycle("af", "af_nursery") {};
    private final Java6GCCycle tenuredAF = new Java6GCCycle("af", "af_tenured") {};
    private final Java6GCCycle systemGC = new Java6GCCycle("sys", "gc_system") {
        @Override
        protected GCState onStartCycle(GCParserContext context, String elementName, String unparsedAttributes) {
            // call super first so context.currentRecord actually exists
            GCState toReturn = super.onStartCycle(context, elementName, unparsedAttributes);
            //
            context.setValue("GCCOUNT", "system_count", "id");
            return toReturn;
        }
    };

    private final Java6GCCycle concurrentGC = new ConcurrentGCCycle();

    public GCState startElement(GCParserContext context, String elementName, String unparsedAttributes) {
        if ("initialized".equals(elementName)) {
            return initialized;
        }
        else {
            // note that the return statements here _call_ startElement() on the next state
            // this is done because the various GC cycles need the current attributes
            context.parseAttributes(unparsedAttributes);

            if ("sys".equals(elementName)) {
                return systemGC.startElement(context, elementName, unparsedAttributes);
            }
            else if ("af".equals(elementName)) {
                String afType = context.getAttribute("type");

                if ("nursery".equals(afType)) {
                    return nurseryAF.startElement(context, elementName, unparsedAttributes);
                }
                else if ("tenured".equals(afType)) {
                    return tenuredAF.startElement(context, elementName, unparsedAttributes);
                }
                else {
                    context.logInvalidValue("type", afType);
                    return this;
                }

            }
            else if ("con".equals(elementName)) {
                return concurrentGC.startElement(context, elementName, unparsedAttributes);
            }
            else {
                context.logUnrecognizedElement(elementName);
                return this;
            }
        }
    }

    public GCState endElement(GCParserContext context, String elementName) {
        if ("verbosegc".equals(elementName)) {
            context.reset();
            reset();

            return Start.INSTANCE;
        }
        else {
            return this;
        }
    }

    @Override
    public void reset() {
        nurseryAF.reset();
        tenuredAF.reset();
        systemGC.reset();
        concurrentGC.reset();
    }

    private Java6GC() {}
}
