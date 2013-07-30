package com.ibm.nmon.file;

public final class NMONFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();

        if (name.endsWith(".nmon")) {
            return true;
        }
        else {
            int idx = name.lastIndexOf('/');

            if (idx != -1) {
                name = pathname.substring(idx + 1);
            }

            return name.contains("nmon")
                    && !(name.endsWith(".zip") || name.endsWith(".gz") || name.endsWith(".tar") || name.endsWith(".7z"));
        }
    }

    public NMONFileFilter() {}
}
