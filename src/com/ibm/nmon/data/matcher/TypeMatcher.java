package com.ibm.nmon.data.matcher;

import java.util.List;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

public interface TypeMatcher {
    public List<DataType> getMatchingTypes(DataSet data);

    /**
     * Matches all {@link DataType DataTypes}.
     */
    public static final TypeMatcher ALL = new TypeMatcher() {
        @Override
        public List<DataType> getMatchingTypes(DataSet data) {
            if ((data == null) || (data.getTypeCount() == 0)) {
                return java.util.Collections.emptyList();
            }
            else {
                List<DataType> toReturn = new java.util.ArrayList<DataType>(data.getTypeCount());

                for (DataType type : data.getTypes()) {
                    toReturn.add(type);
                }

                return toReturn;
            }
        }

        public String toString() {
            return "$ALL";
        };
    };
}
