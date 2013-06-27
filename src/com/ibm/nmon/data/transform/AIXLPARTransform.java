package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;

/**
 * Changes the EC_Idle% and VP_Idle% metric for AIX LPARs to CPU% (i.e. Usr% + Sys%).
 */
public final class AIXLPARTransform implements DataTransform {
    @Override
    public DataType buildDataType(String id, String name, String... fields) {
        String[] newFields = new String[fields.length + 2];

        for (int i = 0, j = 0; i < fields.length; i++, j++) {
            String field = fields[i];

            if (("EC" + "_Idle%").equals(field)) {
                newFields[j] = "EC" + "_CPU%";
            }
            else if (("VP" + "_Idle%").equals(field)) {
                newFields[j] = "VP" + "_CPU%";
            }
            else if ("Folded".equals(field)) {
                newFields[j] = field;
                newFields[++j] = "Unfolded";
            }
            else {
                newFields[j] = field;
            }
        }

        newFields[newFields.length - 1] = "OtherLPARs";

        return new DataType(id, name, newFields);
    }

    @Override
    public double[] transform(DataType type, double[] data) {
        double[] newData = new double[type.getFieldCount()];

        for (int i = 0, j = 0; i < data.length; i++, j++) {
            // replacing idle with busy so the index is the same
            // note data type field name was changed in buildDataType so retrieve with new name!

            String field = type.getField(j);

            if (field.equals("EC" + "_CPU%")) {
                newData[j] = data[type.getFieldIndex("EC" + "_User%")] + data[type.getFieldIndex("EC" + "_Sys%")];
            }
            else if (field.equals("VP" + "_CPU%")) {
                newData[j] = data[type.getFieldIndex("VP" + "_User%")] + data[type.getFieldIndex("VP" + "_Sys%")];
            }
            else if (field.equals("Unfolded")) {
                // Unfolded is the virtual CPU count minus the folded count
                newData[j++] = data[type.getFieldIndex("virtualCPUs")] - data[type.getFieldIndex("Folded")];
            }
            else {
                newData[j] = data[i];
            }
        }

        // OtherLPARs = total pool CPUs - idle CPU - this LPAR's CPU
        newData[newData.length - 1] = data[type.getFieldIndex("poolCPUs")] - data[type.getFieldIndex("PoolIdle")]
                - data[type.getFieldIndex("PhysicalCPU")];

        return newData;
    }

    @Override
    public boolean isValidFor(String typeId) {
        return "LPAR".equals(typeId);
    }
}
