package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;

/**
 * Adds a <code>swapused</code> metric for Linux memory measurements.
 */
public final class LinuxMemoryTransform implements DataTransform {
    @Override
    public DataType buildDataType(String id, String name, String... fields) {
        String[] newFields = new String[fields.length + 1];
        System.arraycopy(fields, 0, newFields, 0, fields.length);

        newFields[fields.length] = "swapused";

        return new DataType(id, name, newFields);
    }

    @Override
    public double[] transform(DataType type, double[] data) {
        double[] newData = new double[data.length + 1];
        System.arraycopy(data, 0, newData, 0, data.length);

        int swaptotal = type.getFieldIndex("swaptotal");
        int swapfree = type.getFieldIndex("swapfree");
        newData[data.length] = newData[swaptotal] - newData[swapfree];

        return newData;
    }

    @Override
    public boolean isValidFor(String typeId) {
        return "MEM".equals(typeId);
    }
}
