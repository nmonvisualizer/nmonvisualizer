package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.parser.gc.GCParserContext;

/**
 * State responsible for parsing <code>&lt;con&gt;</code> elements. These elements need special
 * handling because they can be nested. This implementation only cares about actual garbage
 * collection events.
 */
final class ConcurrentGCCycle extends Java6GCCycle {
    // track nested element depth so actions are only taken at the top level (i.e. 0)
    private int depth = 0;

    // has garbage collection actually started?
    private boolean collectionStarted = false;

    ConcurrentGCCycle() {
        super("con", "con_mark");
    }

    @Override
    protected GCState onStartCycle(GCParserContext context, String elementName, String unparsedAttributes) {
        ++depth;

        // only process 'collection' events, otherwise skip
        if ("collection".equals(context.getAttribute("event"))) {
            calculateTime(context);
            collectionStarted = true;
            error = false;
        }
        else {
            // once collection starts, inner <con> elements are valid
            error = !collectionStarted;
        }

        return this;
    }

    public GCState endElement(GCParserContext context, String elementName) {
        if (elementName.equals(transitionElement)) {
            --depth;

            if (depth == 0) {
                // outermost </con>
                // save data if an actual collection
                // otherwise ignore

                if (collectionStarted) {
                    if (!error) {
                        calculateTotalSizes(context);
                        context.saveRecord();
                    }

                    collectionStarted = false;
                }

                beforeGC = true;
                error = false;

                return Java6GC.INSTANCE;
            }
        }

        // either nested </con> or some other element, continue processing
        return this;
    }

    @Override
    public void reset() {
        super.reset();
        depth = 0;
    }
}
