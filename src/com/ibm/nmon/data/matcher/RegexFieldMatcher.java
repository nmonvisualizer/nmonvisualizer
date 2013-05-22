package com.ibm.nmon.data.matcher;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.nmon.data.DataType;

/**
 * Matches a set of fields based on a regular expression.
 */
public final class RegexFieldMatcher implements FieldMatcher {
    private final Matcher matcher;

    public RegexFieldMatcher(String regex) {
        matcher = Pattern.compile(regex).matcher("");
    }

    @Override
    public List<String> getMatchingFields(DataType type) {
        if (type == null) {
            return java.util.Collections.emptyList();
        }
        else {
            List<String> toReturn = new java.util.ArrayList<String>(type.getFieldCount());

            for (String field : type.getFields()) {
                if (matcher.reset(field).matches()) {
                    toReturn.add(field);
                }
            }

            return toReturn;
        }
    }

    @Override
    public String toString() {
        return matcher.pattern().pattern();
    }
}
