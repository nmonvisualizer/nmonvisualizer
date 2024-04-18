package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;

/**
 * <p>Changes the EC_Idle% and VP_Idle% metric for AIX LPARs to CPU% (i.e. the total).
 * Note that EC_CPU%  <em>cannot</em> exceed 100% even if the LPAR configuration allows CPU sharing.</p>
 * 
 * <p>Adds an EC_Used% value that matches vmstat's <code>ec</code> value. This value <em>can<em> exceed 100%
 * which indicates the LPAR is sharing CPU and currently using more than its entitle capacity.
 * Also adds an OtherLPARs value which is the total CPUS being used by other LPARs plus idle CPUs.</p>
 * 
 * <p>Adds an Unfolded value if Folded exists which is the virtualCPUs - Folded.</p>
 */
public final class AIXLPARTransform implements DataTransform {
    @Override
    public DataType buildDataType(String id, String subId, String name, String... fields) {
        String[] newFields = null;

        if (java.util.Arrays.binarySearch(fields, "Folded") != -1) {
            newFields = new String[fields.length + 3];
        }
        else {
            newFields = new String[fields.length + 2];
        }

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

        newFields[newFields.length - 2] = "EC_Used%";
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
                // per https://www.ibm.com/docs/en/aix/7.3?topic=tool-processor-statistics, total _includes_ Idle%
                newData[j] = data[type.getFieldIndex("EC" + "_User%")] + data[type.getFieldIndex("EC" + "_Sys%")] + data[type.getFieldIndex("EC" + "_Wait%")] + data[type.getFieldIndex("EC" + "_CPU%")];
            }
            else if (field.equals("VP" + "_CPU%")) {
                newData[j] = data[type.getFieldIndex("VP" + "_User%")] + data[type.getFieldIndex("VP" + "_Sys%")];
            }
            else if (field.equals("Unfolded")) {
                // Unfolded is the virtual CPU count minus the folded count
                newData[j] = data[type.getFieldIndex("virtualCPUs")] - data[type.getFieldIndex("Folded")];
                i--;
            }
            else {
                newData[j] = data[i];
            }
        }

        // EC_Used% = PhysicalCPU / entitled (i.e. CPUs in use vs entitled)
        newData[newData.length - 2] = data[type.getFieldIndex("PhysicalCPU")] / data[type.getFieldIndex("entitled")] * 100;

        // OtherLPARs = total pool CPUs - idle CPU - this LPAR's CPU
        newData[newData.length - 1] = data[type.getFieldIndex("poolCPUs")] - data[type.getFieldIndex("PoolIdle")]
                - data[type.getFieldIndex("PhysicalCPU")];

        return newData;
    }

    @Override
    public boolean isValidFor(String typeId, String subId) {
        return "LPAR".equals(typeId);
    }
}
