package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.parser.gc.GCParserContext;

final class Java7GCCycle extends JavaGCCycle {
    static final Java7GCCycle INSTANCE = new Java7GCCycle();

    private int compactionCount;

    @Override
    public GCState startElement(GCParserContext context, String elementName, String unparsedAttributes) {
        if ("mem".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            String type = context.getAttribute("type");

            if ("nursery".equals(type)) {
                calculateSizes(context, type, "free", "total");
            }
            else if ("eden".equals(type)) {
                calculateSizes(context, "nursery", "free", "total");
            }
            else if ("tenure".equals(type)) {
                calculateSizes(context, "tenured", "free", "total");
            }
        }
        else if ("scavenger-info".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            context.setValue("GCSTAT", "tiltratio", "tiltratio");
        }
        else if ("memory-copied".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            String type = context.getAttribute("type");

            if ("nursery".equals(type) || "eden".equals(type)) {
                context.setValue("GCMEM", "flipped", "objects");
                context.setValue("GCMEM", "flipped_bytes", "bytes");
            }
            else if ("tenure".equals(type)) {
                context.setValue("GCMEM", "tenured", "objects");
                context.setValue("GCMEM", "tenured_bytes", "bytes");
            }
        }
        else if ("finalization".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            context.setValue("GCSTAT", "finalizers", "enqueued");
        }
        else if ("references".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            context.setValue("GCSTAT", context.getAttribute("type"), "cleared");
        }
        else if ("compact-info".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            context.setValue("GCCOUNT", "compaction_count", ++compactionCount);

            context.setValue("GCMEM", "moved", "movecount");
            context.setValue("GCMEM", "moved_bytes", "movebytes");
        }
        else if ("concurrent-collection-start".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            context.setValueDiv1000("GCSINCE", "con_mark", "intervalms");
        }
        else if ("gc-op".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            String type = context.getAttribute("type");

            if ("mark".equals(type)) {
                context.setValue("GCTIME", "mark_ms", "timems");
            }
            else if ("sweep".equals(type)) {
                context.setValue("GCTIME", "sweep_ms", "timems");
            }
            else if ("compact".equals(type)) {
                context.setValue("GCTIME", "compact_ms", "timems");
            }
        }
        else if ("remembered-set".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);

            calculateSizes(context, "tenured", "freebytes", "totalbytes");
        }
        else if ("gc-end".equals(elementName)) {
            context.parseAttributes(unparsedAttributes);
            String type = context.getAttribute("type");

            if ("global".equals(type)) {
                context.setValue("GCTIME", "tenured_ms", "durationms");
            }
            else if ("scavenge".equals(type)) {
                context.setValue("GCTIME", "nursery_ms", "durationms");
            }
        }

        return this;
    }

    @Override
    public GCState endElement(GCParserContext context, String elementName) {
        if ("gc-start".equals(elementName)) {
            beforeGC = false;

            return this;
        }
        else if ("cycle-end".equals(elementName)) {
            calculateTotalSizes(context);

            return Java7GC.INSTANCE;
        }
        else {
            return this;
        }
    }

    @Override
    public void reset() {
        super.reset();

        compactionCount = 0;

    }

    private Java7GCCycle() {
        super();
    }
}
