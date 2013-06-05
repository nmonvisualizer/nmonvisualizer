package com.ibm.nmon.file;

public final class HATJFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();
        return name.contains("graph") && name.endsWith(".csv");
    }

    HATJFileFilter() {}
}
