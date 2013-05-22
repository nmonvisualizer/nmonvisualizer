package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataRecord;

import com.ibm.nmon.util.DataHelper;

/**
 * Post processor that adds a <code>NETETOTAL</code> data type. This type will aggregate data for
 * Ethernet interfaces only.
 */
public final class EthernetTotalPostProcessor implements DataPostProcessor {
    private final String typePrefix;

    public EthernetTotalPostProcessor(String typePrefix) {
        if ((typePrefix == null) || typePrefix.equals("")) {
            throw new IllegalArgumentException("typePrefix cannot be null");
        }

        this.typePrefix = typePrefix;
    }

    @Override
    public void addDataTypes(DataSet data) {
        DataType net = data.getType(typePrefix);

        if (net == null) {
            return;
        }

        DataType error = data.getType(typePrefix + "ERROR");
        DataType packet = data.getType(typePrefix + "PACKET");
        DataType size = data.getType(typePrefix + "SIZE");

        boolean hasErrors = error != null;
        boolean hasPackets = packet != null;
        boolean hasSize = size != null;

        int ethernetFieldCount = 3 + (hasErrors ? 4 : 0) + (hasPackets ? 3 : 0) + (hasSize ? 3 : 0);

        int n = 0;
        String[] ethernetFields = new String[ethernetFieldCount];

        ethernetFields[n++] = "total-read-KB/s";
        ethernetFields[n++] = "total-write-KB/s";
        ethernetFields[n++] = "total-KB/s";

        if (hasErrors) {
            ethernetFields[n++] = "total-ierrs";
            ethernetFields[n++] = "total-oerrs";
            ethernetFields[n++] = "total-collisions";
            ethernetFields[n++] = "total-errs";
        }
        if (hasPackets) {
            ethernetFields[n++] = "total-read-packets/s";
            ethernetFields[n++] = "total-write-packets/s";
            ethernetFields[n++] = "total-packets/s";
        }
        if (hasSize) {
            ethernetFields[n++] = "total-readsize";
            ethernetFields[n++] = "total-writesize";
            ethernetFields[n++] = "total-size";
        }

        data.addType(new DataType(typePrefix + "ETOTAL", typePrefix + " Ethernet grand totals", ethernetFields));
    }

    @Override
    public void postProcess(DataSet data, DataRecord record) {
        DataType net = data.getType(typePrefix);

        if (net == null) {
            return;
        }

        if (!record.hasData(net)) {
            return;
        }

        DataType ethernet = data.getType(typePrefix + "ETOTAL");

        DataType error = data.getType(typePrefix + "ERROR");
        DataType packet = data.getType(typePrefix + "PACKET");
        DataType size = data.getType(typePrefix + "SIZE");

        boolean hasErrors = error != null;
        boolean hasPackets = packet != null;
        boolean hasSize = size != null;

        double[] ethernetData = new double[ethernet.getFieldCount()];

        int n = 0;

        for (String iface : DataHelper.getInterfaces(net)) {
            n = 0;

            if (!iface.startsWith("eth") && !iface.startsWith("en")) {
                continue;
            }

            double read = record.getData(net, iface + "-read-KB/s");
            double write = record.getData(net, iface + "-write-KB/s");

            ethernetData[n++] += read;
            ethernetData[n++] += write;
            ethernetData[n++] += read + write;

            if (hasErrors) {
                double ierrs = record.getData(error, iface + "-ierrs");
                double oerrs = record.getData(error, iface + "-oerrs");
                double collisions = record.getData(error, iface + "-collisions");

                ethernetData[n++] += ierrs;
                ethernetData[n++] += oerrs;
                ethernetData[n++] += collisions;
                ethernetData[n++] += ierrs + oerrs + collisions;
            }
            if (hasPackets) {
                read = record.getData(packet, iface + "-reads/s");
                write = record.getData(packet, iface + "-writes/s");

                ethernetData[n++] += read;
                ethernetData[n++] += write;
                ethernetData[n++] += read + write;
            }
            if (hasSize) {
                read = record.getData(size, iface + "-readsize");
                write = record.getData(size, iface + "-writesize");

                ethernetData[n++] += read;
                ethernetData[n++] += write;
                ethernetData[n++] += read + write;
            }
        }

        record.addData(ethernet, ethernetData);
    }
}
