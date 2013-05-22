package com.ibm.nmon.data.matcher;

import java.util.List;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

public final class ExactTypeMatcher implements TypeMatcher {
    private final String type;

    public ExactTypeMatcher(String type) {
        if ((type == null) || "".equals(type)) {
            throw new IllegalArgumentException("type cannot be null");
        }

        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public List<DataType> getMatchingTypes(DataSet data) {
        if ((data == null) || (data.getTypeCount() == 0)) {
            return java.util.Collections.emptyList();
        }
        else {
            List<DataType> toReturn = new java.util.ArrayList<DataType>(data.getTypeCount());

            for (DataType toCompare : data.getTypes()) {
                // note matching on toString, not typeId
                if (type.equals(toCompare.toString())) {
                    toReturn.add(toCompare);
                }
            }

            return toReturn;
        }
    }

    @Override
    public String toString() {
        return type;
    }
}
