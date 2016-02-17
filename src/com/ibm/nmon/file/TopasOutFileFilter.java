package com.ibm.nmon.file;

public final class TopasOutFileFilter extends BaseFileFilter {
    @Override
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();

        return name.contains("topas")
                && !(name.endsWith(".zip") || name.endsWith(".gz") || name.endsWith(".tar") || name.endsWith(".7z"));
    }

    public TopasOutFileFilter() {}
}
