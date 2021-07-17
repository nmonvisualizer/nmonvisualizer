package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.parser.gc.GCParserContext;

final class Initialized extends GCStateWithParent {
    Initialized(GCState parent) {
        super("initialized", parent);
    }

    public GCState startElement(GCParserContext context, String elementName, String unparsedAttributes) {
        if ("attribute".equals(elementName)) {
            // save attributes directly as metadata
            context.parseAttributes(unparsedAttributes);

            String name = context.getAttribute("name");
            String value = context.getAttribute("value");

            if (name == null) {
                context.logMissingAttribute("name");
            }
            else if (value == null) {
                context.logMissingAttribute("value");
            }
            else {
                context.getData().setMetadata(name, value);

                if ("gcPolicy".equals(name)) {
                    context.setGencon("-Xgcpolicy:gencon".equals(value) || "-Xgcpolicy:balanced".equals(value));
                }
            }
        }
        else if ("vmarg".equals(elementName)) {
            // parse vmargs and save the values to a single string
            context.parseAttributes(unparsedAttributes);

            String name = context.getAttribute("name");

            if (name == null) {
                context.logMissingAttribute("name");
            }
            else {
                String vmargs = context.getData().getMetadata("vmargs");

                if (vmargs == null) {
                    vmargs = name;
                }
                else {
                    vmargs += "\n\t" + name;
                }

                context.getData().setMetadata("vmargs", vmargs);
            }
        }

        return this;
    }
}
