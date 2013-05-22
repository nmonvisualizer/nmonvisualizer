package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataRecord;

/**
 * An interface defining a methods for the parser to modify data from the file after each DataRecord
 * is parsed. Compared to a {@link DataTransform}, this interface is designed to create new or
 * aggregate DataTypes from one or many existing types, rather than working with the data for a
 * single record / type combination.
 */
public interface DataPostProcessor {
    public void addDataTypes(DataSet data);

    public void postProcess(DataSet data, DataRecord record);
}
