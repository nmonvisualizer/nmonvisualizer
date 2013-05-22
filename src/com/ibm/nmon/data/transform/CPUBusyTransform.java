package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;

/**
 * Changes the Idle% metric for CPU measurements to CPU% (i.e. Usr% + Sys%). Also modifies the AIX
 * <code>PhysicalCPUs</code> value to <code>CPUs</code>, the same as Linux.
 */
public final class CPUBusyTransform implements DataTransform {
    @Override
    public DataType buildDataType(String id, String name, String... fields) {
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];

            if ("Idle%".equals(field)) {    
                fields[i] = "CPU%";
            }
            else if ("PhysicalCPUs".equals(field)) {
                // rename AIX value to line up with LINUX
                fields[i] = "CPUs";
            }
        }

        return new DataType(id, name, fields);
    }

    @Override
    public double[] transform(DataType type, double[] data) {
        // replacing idle with busy so the index is the same
        // note data type field name was changed in buildDataType so retrieve with new name!
        int idx = type.getFieldIndex("CPU%");
        data[idx] = data[type.getFieldIndex("User%")] + data[type.getFieldIndex("Sys%")];

        return data;
    }

    @Override
    public boolean isValidFor(String typeId) {
        return typeId.startsWith("CPU");
    }
}
