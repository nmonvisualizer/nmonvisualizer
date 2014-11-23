package com.ibm.nmon.file;

public final class PerfmonFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();
        return name.endsWith(".csv");
    }

    public PerfmonFileFilter() {}
}
