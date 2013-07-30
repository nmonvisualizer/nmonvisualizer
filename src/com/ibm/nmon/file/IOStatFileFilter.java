package com.ibm.nmon.file;

public final class IOStatFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();

        if (name.endsWith(".iostat")) {
            return true;
        }
        else {
            int idx = name.lastIndexOf('/');

            if (idx != -1) {
                name = pathname.substring(idx + 1);
            }

            return name.contains("iostat")
                    && !(name.endsWith(".zip") || name.endsWith(".gz") || name.endsWith(".tar") || name.endsWith(".7z"));
        }
    }

    IOStatFileFilter() {}
}
