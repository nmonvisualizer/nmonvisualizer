package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.parser.gc.GCParserContext;

/**
 * State responsible for parsing <code>&lt;gc&gt;</code> elements.
 */
final class Java6Collection extends GCStateWithParent {
    // either scavenger (nursery) or global (tenured)
    private String type;

    // tenured element shows up twice
    // use this as a flag to indicate which <tenured> element is being parsed
    private boolean tenuredCountComplete;

    @Override
    public GCState startElement(GCParserContext context, String elementName, String unparsedAttributes) {
        context.parseAttributes(unparsedAttributes);

        if ("gc".equals(elementName)) {
            this.type = context.getAttribute("type");
            tenuredCountComplete = false;

            if ("scavenger".equals(type)) {
                context.setValueDiv1000("GCSINCE", "gc_scavenger", "intervalms");
                context.setValue("GCCOUNT", "nursery_count", "id");
                context.setValue("GCCOUNT", "total_count", "totalid");
            }
            else if ("global".equals(type)) {
                context.setValueDiv1000("GCSINCE", "gc_global", "intervalms");
                context.setValue("GCCOUNT", "tenured_count", "id");
                context.setValue("GCCOUNT", "total_count", "totalid");

                // global GC does not move objects from nursery to tenured
                tenuredCountComplete = true;
            }
            else {
                context.logInvalidValue("type", type);
                // valid Java, but somewhat of a hack since error is protected
                // but much easier that communicating up to the state that cares about errors
                ((JavaGCCycle) parent).error = true;
            }
        }
        else if ("flipped".equals(elementName)) {
            context.setValue("GCMEM", "flipped", "objectcount");
            context.setValue("GCMEM", "flipped_bytes", "bytes");
        }
        else if ("tenured".equals(elementName)) {
            if (!tenuredCountComplete) {
                context.setValue("GCMEM", "tenured", "objectcount");
                context.setValue("GCMEM", "tenured_bytes", "bytes");
                tenuredCountComplete = true;
            }
            else {
                // IGNORING <tenured> inside of <gc>; this is the data after GC but before an
                // allocation failure is fulfilled. We do not care about this data since
                // Java6GCCycle.calculateTotalSizes() handles that case using the <tenured> data
                // after </gc>.
                tenuredCountComplete = false;
            }
        }
        else if ("finalization".equals(elementName)) {
            context.setValue("GCSTAT", "finalizers", "objectsqueued");
        }
        else if ("compaction".equals(elementName)) {
            context.setValue("GCMEM", "moved", "movecount");
            context.setValue("GCMEM", "moved_bytes", "movebytes");
            context.setValue("GCCOUNT", "compaction_count", context.incrementCompactionCount());
        }
        // TODO classunloading
        else if ("scavenger".equals(elementName)) {
            context.setValue("GCSTAT", "tiltratio", "tiltratio");
        }
        else if ("time".equals(elementName)) {
            if ("scavenger".equals(type)) {
                context.setValue("GCTIME", "nursery_ms", "totalms");
            }
            // tenured time is in 'timesms' element
        }
        else if ("timesms".equals(elementName)) {
            context.setValue("GCTIME", "tenured_ms", "total");
            context.setValue("GCTIME", "mark_ms", "mark");
            context.setValue("GCTIME", "sweep_ms", "sweep");
            context.setValue("GCTIME", "compact_ms", "compact");
        }

        return this;
    }

    Java6Collection(Java6GCCycle parent) {
        super("gc", parent);
    }
}
