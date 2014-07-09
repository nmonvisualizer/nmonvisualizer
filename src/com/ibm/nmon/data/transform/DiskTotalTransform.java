package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;
import com.ibm.nmon.util.DataHelper;

/**
 * Adds a 'Total' column to disk statistics so that the values across all disks in a system can be
 * analyzed. This transform skips partitions under the assumption that there will be an already
 * existing parent disk metric that aggregates measurements for all the partitions in a disk.
 */
public final class DiskTotalTransform implements DataTransform {
    @Override
    public DataType buildDataType(String id, String subId, String name, String... fields) {
        String[] newFields = new String[fields.length + 1];
        System.arraycopy(fields, 0, newFields, 0, fields.length);

        newFields[fields.length] = "Total";

        return new DataType(id, name, newFields);
    }

    @Override
    public double[] transform(DataType type, double[] data) {
        double[] newData = new double[data.length + 1];
        System.arraycopy(data, 0, newData, 0, data.length);

        double total = 0;

        for (String field : type.getFields()) {
            if (DataHelper.isNotPartition(field)) {
                int idx = type.getFieldIndex(field);

                if (idx < data.length) {
                    total += data[idx];
                }
                else {
                    // disks could have been removed, assume all subsequent disks are missing too
                    // and just take the total as-is
                    break;
                }
            }
        }

        newData[data.length] = total;

        return newData;
    }

    @Override
    public boolean isValidFor(String typeId, String subId) {
        return typeId.startsWith("DISK") && !typeId.equals("DISKBUSY");
    }
}
