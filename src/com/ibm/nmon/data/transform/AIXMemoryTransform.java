package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;

/**
 * Adds <code>used</code> columns to AIX MEM statistics in addition to the default <code>free</code>
 * data.
 */
public final class AIXMemoryTransform implements DataTransform {
    @Override
    public DataType buildDataType(String id, String subId, String name, String... fields) {
        String[] newFields = new String[fields.length + 4];

        // Real Free % and Virtual free %
        newFields[0] = fields[0];
        newFields[1] = fields[1];

        newFields[2] = "Real used %";
        newFields[3] = "Virtual used %";

        // Real free(MB) and Virtual free(MB)
        newFields[4] = fields[2];
        newFields[5] = fields[3];

        newFields[6] = "Real used(MB)";
        newFields[7] = "Virtual used(MB)";

        for (int i = 4; i < fields.length; i++) {
            newFields[i + 4] = fields[i];
        }

        return new DataType(id, name, newFields);
    }

    @Override
    public double[] transform(DataType type, double[] data) {
        double[] newData = new double[data.length + 4];

        newData[0] = data[0];
        newData[1] = data[1];

        // free % = 100 - used
        newData[2] = 100 - data[0];
        newData[3] = 100 - data[1];

        newData[4] = data[2];
        newData[5] = data[3];

        // free = total - used
        newData[6] = data[4] - data[2];
        newData[7] = data[5] - data[3];

        for (int i = 4; i < data.length; i++) {
            newData[i + 4] = data[i];
        }

        return newData;
    }

    @Override
    public boolean isValidFor(String typeId, String subId) {
        return "MEM".equals(typeId);
    }
}
