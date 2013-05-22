package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.parser.gc.GCParserContext;

public interface GCState {
    public GCState startElement(GCParserContext context, String elementName, String unparsedAttributes);

    public GCState endElement(GCParserContext context, String elementName);

    public void reset();
}
