package com.ibm.nmon.file;

public final class GCFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();

        int idx = name.lastIndexOf('/');

        if (idx != -1) {
            name = name.substring(idx + 1);
        }

        return name.contains("verbose") || name.contains("native_stderr");
    }

    GCFileFilter() {}
}
