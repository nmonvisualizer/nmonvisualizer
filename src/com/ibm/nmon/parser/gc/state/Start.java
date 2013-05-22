package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.parser.gc.GCParserContext;
import com.ibm.nmon.parser.util.XMLParserHelper;

/**
 * The initial GCState that looks for <code>&lt;verbosegc&gt;</code> elements. Transitions to the
 * appropriate Java GC state based on the version of the verbose GC log being parsed.
 */
public final class Start implements GCState {
    public static final Start INSTANCE = new Start();

    @Override
    public GCState startElement(GCParserContext context, String elementName, String unparsedAttributes) {
        if ("verbosegc".equals(elementName)) {
            if (XMLParserHelper.parseAttributes(unparsedAttributes).get("xmlns") != null) {
                return Java7GC.INSTANCE;
            }
            else {
                return Java6GC.INSTANCE;
            }
        }
        else {
            return this;
        }
    }

    @Override
    public GCState endElement(GCParserContext context, String elementName) {
        return this;
    }

    @Override
    public void reset() {
        Java6GC.INSTANCE.reset();
        Java7GC.INSTANCE.reset();
    }

    private Start() {}
}
