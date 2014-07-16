package com.ibm.nmon.data.transform;

import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;

/**
 * Post processor to add a <code>Network Interface (Total)</code> data type that aggregates data for
 * all network interfaces.
 */
public final class WindowsNetworkPostProcessor implements DataPostProcessor {
    @Override
    public void addDataTypes(DataSet data) {
        SubDataType total = null;
        for (DataType type : data.getTypes()) {
            if (type.getId().contains("Network Interface")) {
                total = new SubDataType("Network Interface", "Total", "Network Interface", type.getFields().toArray(
                        new String[type.getFieldCount()]));
                break;
            }
        }

        if (total != null) {
            data.addType(total);
        }
    }

    @Override
    public void postProcess(DataSet data, DataRecord record) {
        DataType total = data.getType("Network Interface" + " (Total)");

        if (total == null) {
            return;
        }

        double[] totalData = new double[total.getFieldCount()];

        for (int i = 0; i < totalData.length; i++) {
            totalData[i] = 0;
        }

        for (DataType type : data.getTypes()) {
            if (type.getId().contains("Network Interface") && (type != total)) {
                if (record.hasData(type)) {
                    double[] typeData = record.getData(type);

                    // assume ordering is the same for all types
                    for (int i = 0; i < total.getFieldCount(); i++) {
                        totalData[i] += typeData[i];
                    }
                }
            }
        }

        record.addData(total, totalData);
    }
}
