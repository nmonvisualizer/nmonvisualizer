package com.ibm.nmon.data.matcher;

import java.util.List;
import java.util.Set;

import com.ibm.nmon.data.DataType;

/**
 * Matches a defined set of fields. When matching a number of fields, this class is more efficient
 * than using a set of {@link ExactFieldMatcher ExactFieldMatchers}.
 */
public final class SetFieldMatcher implements FieldMatcher {
    private final Set<String> fields;

    public SetFieldMatcher(String... fields) {
        if ((fields == null) || (fields.length == 0)) {
            throw new IllegalArgumentException("fields cannot be empty");
        }

        Set<String> temp = new java.util.HashSet<String>(java.util.Arrays.asList(fields));
        this.fields = java.util.Collections.unmodifiableSet(temp);
    }

    @Override
    public List<String> getMatchingFields(DataType type) {
        if (type == null) {
            return java.util.Collections.emptyList();
        }
        else {
            List<String> toReturn = new java.util.ArrayList<String>(type.getFieldCount());

            for (String field : type.getFields()) {
                if (fields.contains(field)) {
                    toReturn.add(field);
                }
            }

            return toReturn;
        }
    }

    @Override
    public String toString() {
        return fields.toString();
    }
}
