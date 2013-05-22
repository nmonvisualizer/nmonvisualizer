package com.ibm.nmon.data.matcher;

import java.util.Collection;
import java.util.List;

import com.ibm.nmon.data.DataSet;

/**
 * Matches a {@link DataSet DataSets} based on <em>hostname</em>.
 * 
 * @see DataSet#getHostname()
 */
public interface HostMatcher {
    public boolean matchesHost(DataSet data);

    public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch);

    /**
     * Matches all hostnames.
     */
    public static final HostMatcher ALL = new HostMatcher() {
        @Override
        public boolean matchesHost(DataSet data) {
            return true;
        }

        @Override
        public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch) {
            return new java.util.ArrayList<DataSet>(toMatch);
        }

        public String toString() {
            return "$ALL";
        };
    };
}
