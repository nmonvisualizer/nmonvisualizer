package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;

/**
 * An interface defining methods for a parser to modify data from the file as it is being parsed. A
 * given DataTransform may add, remove, or change fields.
 */
public interface DataTransform {
    /**
     * Create a DataType based on transformed data. The given fields may be added, removed or
     * renamed.
     * 
     * @param id the DataType id
     * @param name the name of the DataType
     * @param fields the original fields from the parsed file
     * 
     * @return a new, transformed DataType
     */
    public DataType buildDataType(String id, String name, String... fields);

    /**
     * Transform the given data according to the DataType. This DataType will be the same as created
     * by a previous call to <code>buildDataType</code>.
     * 
     * @param type the DataType to transform
     * @param data the orginal data from the parsed file
     * 
     * @return the transformed data array, which may be a different size than the original
     */
    public double[] transform(DataType type, double[] data);

    /**
     * Can this DataTransform manipulate data for the given DataType id? Transforms can be valid for
     * multiple DataTypes.
     * 
     * @param typeId
     * 
     * @return true if the parser should apply this transform for the given DataType
     */
    public boolean isValidFor(String typeId);
}
