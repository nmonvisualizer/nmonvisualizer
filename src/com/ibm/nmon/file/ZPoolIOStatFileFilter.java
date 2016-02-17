package com.ibm.nmon.file;

public final class ZPoolIOStatFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();

        return name.contains("zpool")
                && !(name.endsWith(".zip") || name.endsWith(".gz") || name.endsWith(".tar") || name.endsWith(".7z"));
    }

    ZPoolIOStatFileFilter() {}
}
