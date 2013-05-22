package com.ibm.nmon.file;

public final class IOStatFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();
        return name.contains("iostat");
    }

    IOStatFileFilter() {}
}
