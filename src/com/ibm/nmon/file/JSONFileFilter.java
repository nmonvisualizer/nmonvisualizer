package com.ibm.nmon.file;

public final class JSONFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();
        return name.endsWith(".json");
    }

    JSONFileFilter() {};
}
