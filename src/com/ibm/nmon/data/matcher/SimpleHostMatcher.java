package com.ibm.nmon.data.matcher;

import java.util.Collection;
import java.util.List;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.NMONDataSet;
import com.ibm.nmon.data.PerfmonDataSet;
import com.ibm.nmon.data.SystemDataSet;

abstract class SimpleHostMatcher implements HostMatcher {
    private String name;

    SimpleHostMatcher(String name) {
        this.name = name;
    }

    @Override
    public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch) {
        java.util.List<DataSet> toReturn = new java.util.ArrayList<DataSet>(toMatch.size());

        for (DataSet data : toMatch) {
            if (matchesHost(data)) {
                toReturn.add(data);
            }
        }

        return toReturn;
    }

    protected final String getMetadata(DataSet data, String key) {
        String value = null;

        if (data.getClass().equals(BasicDataSet.class)) {
            value = ((BasicDataSet) data).getMetadata(key);
        }
        else if (data.getClass().equals(NMONDataSet.class)) {
            value = ((NMONDataSet) data).getMetadata(key);
        }
        else if (data.getClass().equals(PerfmonDataSet.class)) {
            value = ((PerfmonDataSet) data).getMetadata(key);
        }
        else if (data.getClass().equals(SystemDataSet.class)) {
            SystemDataSet systemData = (SystemDataSet) data;

            for (long time : systemData.getMetadataTimes()) {
                java.util.Map<String, String> metadata = systemData.getMetadata(time);

                if (metadata != null) {
                    value = metadata.get(key);

                    if (value != null) {
                        break;
                    }
                }
            }
        }

        if (value == null) {
            value = "";
        }

        return value.toLowerCase();
    }

    @Override
    public String toString() {
        return name;
    }
}