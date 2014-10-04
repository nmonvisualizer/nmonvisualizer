package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;

/**
 * Adds a 'Total' column to PCPU and SCPU data in AIX.
 */
public final class AIXCPUTransform implements DataTransform {
    @Override
    public DataType buildDataType(String id, String subId, String name, String... fields) {
        String[] newFields = new String[fields.length + 1];

        // Assume User, Sys, Wait, Idle + Entitled Capacity on PCPU
        System.arraycopy(fields, 0, newFields, 0, 4);

        if (fields.length == 4) {
            newFields[4] = "Total";
        }
        else {
            newFields[5] = fields[4];
            newFields[4] = "Total";
        }

        return new DataType(id, name, newFields);
    }

    @Override
    public double[] transform(DataType type, double[] data) {
        double[] newData = new double[data.length + 1];

        System.arraycopy(data, 0, newData, 0, 4);

        if (data.length == 5) {
            newData[5] = data[4];
        }

        double total = 0;

        for (int i = 0; i < 4; i++) {
            total += data[i];
        }

        newData[4] = total;

        return newData;
    }

    @Override
    public boolean isValidFor(String typeId, String subId) {
        return typeId.startsWith("SCPU") || typeId.startsWith("PCPU");
    }
}
