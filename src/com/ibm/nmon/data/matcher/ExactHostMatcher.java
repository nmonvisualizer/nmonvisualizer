package com.ibm.nmon.data.matcher;

import java.util.Collection;
import java.util.List;

import com.ibm.nmon.data.DataSet;

public final class ExactHostMatcher implements HostMatcher {
    private final String hostname;

    public ExactHostMatcher(String hostname) {
        if ((hostname == null) || "".equals(hostname)) {
            throw new IllegalArgumentException("hostname cannot be null");
        }

        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    @Override
    public boolean matchesHost(DataSet data) {
        return data.getHostname().equals(hostname);
    }

    @Override
    public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch) {
        if ((toMatch == null) || toMatch.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        else {
            List<DataSet> toReturn = new java.util.ArrayList<DataSet>(toMatch.size());

            for (DataSet data : toMatch) {
                if (data.getHostname().equals(hostname)) {
                    toReturn.add(data);
                }
            }

            return toReturn;
        }
    }

    @Override
    public String toString() {
        return hostname;
    }
}
