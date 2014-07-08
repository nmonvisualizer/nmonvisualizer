package com.ibm.nmon.data.matcher;

import java.util.Collection;
import java.util.List;

import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.NMONDataSet;
import com.ibm.nmon.data.SystemDataSet;

/**
 * Matches a {@link DataSet} based on the operating system. This class relies on the correct
 * metadata being set by the parser.
 */
public final class OSMatcher implements HostMatcher {
    private final HostMatcher matcher;

    public OSMatcher(String operatingSystem) {
        if ((operatingSystem == null) || "".equals(operatingSystem)) {
            throw new IllegalArgumentException("operatingSystem cannot be null");
        }

        operatingSystem = operatingSystem.toLowerCase();

        if (operatingSystem.contains("linux")) {
            matcher = LINUX;
        }
        else if (operatingSystem.contains("aix")) {
            matcher = AIX;
        }
        else if (operatingSystem.contains("vios")) {
            matcher = VIOS;
        }
        else if (operatingSystem.contains("perfmon")) {
            matcher = PERFMON;
        }
        else {
            matcher = UNKNOWN;
        }
    }

    @Override
    public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch) {
        if ((toMatch == null) || toMatch.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        else {
            List<DataSet> toReturn = new java.util.ArrayList<DataSet>(toMatch.size());

            for (DataSet data : toMatch) {
                if (matcher.matchesHost(data)) {
                    toReturn.add(data);
                }
            }

            return toReturn;
        }
    }

    @Override
    public boolean matchesHost(DataSet data) {
        return matcher.matchesHost(data);
    }

    @Override
    public String toString() {
        return matcher.toString();
    }

    @Override
    public int hashCode() {
        return matcher.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        else if (obj instanceof OSMatcher) {
            OSMatcher matcher = (OSMatcher) obj;

            return this.matcher == matcher.matcher;
        }
        else {
            return false;
        }
    }

    private static final String getMetadata(DataSet data, String key) {
        String value = null;

        if (data.getClass().equals(BasicDataSet.class)) {
            value = ((BasicDataSet) data).getMetadata(key);
        }
        else if (data.getClass().equals(NMONDataSet.class)) {
            value = ((NMONDataSet) data).getMetadata(key);
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

    private static final HostMatcher LINUX = new HostMatcher() {
        @Override
        public boolean matchesHost(DataSet data) {
            return getMetadata(data, "OS").contains("linux");
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

        public String toString() {
            return "linux";
        };
    };

    private static final HostMatcher AIX = new HostMatcher() {
        @Override
        public boolean matchesHost(DataSet data) {
            return !getMetadata(data, "AIX").equals("");
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

        public String toString() {
            return "aix";
        };
    };

    private static final HostMatcher VIOS = new HostMatcher() {
        @Override
        public boolean matchesHost(DataSet data) {
            return !getMetadata(data, "VIOS").equals("");
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

        public String toString() {
            return "vios";
        };
    };

    private static final HostMatcher PERFMON = new HostMatcher() {
        @Override
        public boolean matchesHost(DataSet data) {
            return getMetadata(data, "OS").contains("Perfmon");
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

        public String toString() {
            return "perfmon";
        };
    };

    private static final HostMatcher UNKNOWN = new HostMatcher() {
        @Override
        public boolean matchesHost(DataSet data) {
            return false;
        }

        @Override
        public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch) {
            return java.util.Collections.emptyList();
        }

        public String toString() {
            return "unknown";
        };
    };
}
