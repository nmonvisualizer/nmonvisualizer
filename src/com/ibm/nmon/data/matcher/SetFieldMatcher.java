package com.ibm.nmon.data.matcher;

import java.util.List;

import com.ibm.nmon.data.DataType;

/**
 * Matches a defined set of fields. When matching a number of fields, this class is more efficient
 * than using a set of {@link ExactFieldMatcher ExactFieldMatchers}.
 */
public final class SetFieldMatcher implements FieldMatcher {
    private final List<String> fields;

    public SetFieldMatcher(String... fields) {
        if ((fields == null) || (fields.length == 0)) {
            throw new IllegalArgumentException("fields cannot be empty");
        }

        List<String> temp = new java.util.ArrayList<String>(java.util.Arrays.asList(fields));
        this.fields = java.util.Collections.unmodifiableList(temp);
    }

    @Override
    public List<String> getMatchingFields(DataType type) {
        if (type == null) {
            return java.util.Collections.emptyList();
        }
        else {
            List<String> toReturn = new java.util.ArrayList<String>(fields.size());

            for (String field : fields) {
                if (type.hasField(field)) {
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

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        else if (obj instanceof SetFieldMatcher) {
            SetFieldMatcher matcher = (SetFieldMatcher) obj;

            return this.fields.equals(matcher.fields);
        }
        else {
            return false;
        }
    }
}
