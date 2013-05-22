package com.ibm.nmon.file;

public final class NMONFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        if (pathname.endsWith(".nmon")) {
            return true;
        }
        else {
            int idx = pathname.lastIndexOf('/');

            if (idx == -1) {
                return pathname.contains("nmon");
            }
            else {
                return pathname.substring(idx + 1).contains("nmon");
            }
        }
    }

    public NMONFileFilter() {}
}
