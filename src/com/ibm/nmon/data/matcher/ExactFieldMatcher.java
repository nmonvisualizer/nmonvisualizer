package com.ibm.nmon.data.matcher;

import java.util.List;

import com.ibm.nmon.data.DataType;

public final class ExactFieldMatcher implements FieldMatcher {
    private final String field;

    public ExactFieldMatcher(String field) {
        if ((field == null) || "".equals(field)) {
            throw new IllegalArgumentException("field cannot be null");
        }

        this.field = field;
    }

    public String getField() {
        return field;
    }

    @Override
    public List<String> getMatchingFields(DataType type) {
        if (type == null) {
            return java.util.Collections.emptyList();
        }
        else {
            List<String> toReturn = new java.util.ArrayList<String>(type.getFieldCount());

            for (String field : type.getFields()) {
                if (this.field.equals(field)) {
                    toReturn.add(field);
                }
            }

            return toReturn;
        }
    }

    @Override
    public String toString() {
        return field;
    }
}
