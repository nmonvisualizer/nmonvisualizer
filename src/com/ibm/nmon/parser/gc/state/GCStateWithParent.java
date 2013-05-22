package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.parser.gc.GCParserContext;

/**
 * Helper state that transitions back to a parent state when a given end element is encountered.
 */
class GCStateWithParent implements GCState {
    protected final String transitionElement;
    protected final GCState parent;

    GCStateWithParent(String transitionElement, GCState parent) {
        this.transitionElement = transitionElement;
        this.parent = parent;
    }

    public GCState startElement(GCParserContext context, String elementName, String unparsedAttributes) {
        return this;
    }

    public GCState endElement(GCParserContext context, String elementName) {
        if (elementName.equals(transitionElement)) {
            return parent;
        }
        else {
            return this;
        }
    }

    @Override
    public void reset() {}
}
