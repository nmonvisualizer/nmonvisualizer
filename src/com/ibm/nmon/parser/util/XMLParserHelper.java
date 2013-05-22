package com.ibm.nmon.parser.util;

import java.util.Map;

import java.util.regex.Pattern;

import com.ibm.nmon.util.DataHelper;

public final class XMLParserHelper {
    protected static final Pattern ATTRIBUTE_SPLIITTER = Pattern.compile("=?\" ?");

    public static Map<String, String> parseAttributes(String unparsedAttributes) {
        if ((unparsedAttributes == null) || "".equals(unparsedAttributes)) {
            return java.util.Collections.emptyMap();
        }

        String[] parts = ATTRIBUTE_SPLIITTER.split(unparsedAttributes);

        int size = parts.length / 2;

        Map<String, String> attributes = new java.util.HashMap<String, String>(size);

        // drop odd numbered end index from array
        size *= 2;

        for (int i = 0; i < size;) {
            attributes.put(DataHelper.newString(parts[i++].trim()), DataHelper.newString(parts[i++].trim()));
        }

        return attributes;
    }

    private XMLParserHelper() {}
}
