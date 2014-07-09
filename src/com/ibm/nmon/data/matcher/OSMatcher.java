package com.ibm.nmon.data.matcher;

import java.util.Collection;
import java.util.List;

import com.ibm.nmon.data.DataSet;

/**
 * Matches a {@link DataSet} based on the operating system. This class relies on the correct
 * metadata being set by the parser.
 */
public final class OSMatcher extends SimpleHostMatcher {
    private final HostMatcher matcher;

    public OSMatcher(String operatingSystem) {
        super("os");

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
        else if (operatingSystem.contains("unix")) {
            matcher = UNIX;
        }
        else if (operatingSystem.contains("perfmon")) {
            matcher = PERFMON;
        }
        else {
            matcher = UNKNOWN;
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

    private static final HostMatcher LINUX = new SimpleHostMatcher("linux") {
        @Override
        public boolean matchesHost(DataSet data) {
            return getMetadata(data, "OS").contains("linux");
        }
    };

    private static final HostMatcher AIX = new SimpleHostMatcher("aix") {
        @Override
        public boolean matchesHost(DataSet data) {
            return !getMetadata(data, "AIX").equals("");
        }
    };

    private static final HostMatcher VIOS = new SimpleHostMatcher("vios") {
        @Override
        public boolean matchesHost(DataSet data) {
            return !getMetadata(data, "VIOS").equals("");
        }
    };

    private static final HostMatcher UNIX = new SimpleHostMatcher("unix") {
        @Override
        public boolean matchesHost(DataSet data) {
            return LINUX.matchesHost(data) || AIX.matchesHost(data) || VIOS.matchesHost(data);
        }
    };

    private static final HostMatcher PERFMON = new SimpleHostMatcher("perfmon") {
        @Override
        public boolean matchesHost(DataSet data) {
            return getMetadata(data, "OS").equals("perfmon");
        }
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
