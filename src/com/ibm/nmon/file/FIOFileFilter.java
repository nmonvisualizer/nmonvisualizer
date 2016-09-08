package com.ibm.nmon.file;

public final class FIOFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();
        return name.endsWith(".log");
    }

    public FIOFileFilter() {}
}
