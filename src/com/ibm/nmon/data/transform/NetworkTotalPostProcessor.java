package com.ibm.nmon.data.transform;

import java.util.Set;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataRecord;

import com.ibm.nmon.util.DataHelper;

/**
 * Post processor that adds a <code>TOTAL</code> data type for network data. This type will have
 * <code>KB/s</code>, <code>errs</code>, <code>packets</code> and <code>size</code> (KB per packet)
 * fields for each interface if the data exists for each interface.
 */
public final class NetworkTotalPostProcessor implements DataPostProcessor {
    private final String typePrefix;

    public NetworkTotalPostProcessor(String typePrefix) {
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

        Set<String> ifaces = DataHelper.getInterfaces(net);

        int totalFieldCount = (1 + (hasErrors ? 1 : 0) + (hasPackets ? 1 : 0) + (hasSize ? 1 : 0)) * ifaces.size();
        String[] totalFields = new String[totalFieldCount];

        int n = 0;

        for (String iface : ifaces) {
            totalFields[n++] = iface + "-KB/s";

            if (hasErrors) {
                totalFields[n++] = iface + "-errs";
            }
            if (hasPackets) {
                totalFields[n++] = iface + "-packets/s";
            }
            if (hasSize) {
                totalFields[n++] = iface + "-size";
            }
        }

        data.addType(new DataType(typePrefix + "TOTAL", typePrefix + " Totals", totalFields));
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

        DataType total = data.getType(typePrefix + "TOTAL");

        DataType error = data.getType(typePrefix + "ERROR");
        DataType packet = data.getType(typePrefix + "PACKET");
        DataType size = data.getType(typePrefix + "SIZE");

        boolean hasErrors = error != null;
        boolean hasPackets = packet != null;
        boolean hasSize = size != null;

        Set<String> ifaces = DataHelper.getInterfaces(net);

        double[] totalData = new double[total.getFieldCount()];

        int n = 0;

        for (String iface : ifaces) {
            double read = record.getData(net, iface + "-read-KB/s");
            double write = record.getData(net, iface + "-write-KB/s");

            totalData[n++] = read + write;

            if (hasErrors) {
                double ierrs = record.getData(error, iface + "-ierrs");
                double oerrs = record.getData(error, iface + "-oerrs");
                double collisions = record.getData(error, iface + "-collisions");

                totalData[n++] = ierrs + oerrs + collisions;
            }
            if (hasPackets) {
                read = record.getData(packet, iface + "-reads/s");
                write = record.getData(packet, iface + "-writes/s");

                totalData[n++] = read + write;
            }
            if (hasSize) {
                read = record.getData(size, iface + "-readsize");
                write = record.getData(size, iface + "-writesize");

                totalData[n++] = read + write;
            }
        }

        record.addData(total, totalData);
    }
}
