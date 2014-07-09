package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataType;

/**
 * Make Linux NETPACKET consistent with AIX where the fields is 'reads/s' vs 'read/s'. This is
 * needed for the network post processors to function correctly.
 */
public final class LinuxNetPacketTransform implements DataTransform {
    @Override
    public DataType buildDataType(String id, String subId, String name, String... fields) {
        for (int i = 0; i < fields.length; i++) {
            int idx = fields[i].indexOf("read/s");

            if (idx != -1) {
                fields[i] = fields[i].substring(0, idx) + "reads/s";
            }

            idx = fields[i].indexOf("write/s");

            if (idx != -1) {
                fields[i] = fields[i].substring(0, idx) + "writes/s";
            }
        }

        return new DataType(id, name, fields);
    }

    @Override
    public double[] transform(DataType type, double[] data) {
        return data;
    }

    @Override
    public boolean isValidFor(String typeId, String subId) {
        return typeId.startsWith("NETPACKET");
    }
}
