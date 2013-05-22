package com.ibm.nmon.data.matcher;

import java.util.List;

import com.ibm.nmon.data.DataType;

public interface FieldMatcher {
    public List<String> getMatchingFields(DataType type);

    /**
     * Matches all fields.
     */
    public static final FieldMatcher ALL = new FieldMatcher() {
        @Override
        public List<String> getMatchingFields(DataType type) {
            if (type == null) {
                return java.util.Collections.emptyList();
            }
            else {
                return type.getFields();
            }
        }

        public String toString() {
            return "$ALL";
        };
    };
}
