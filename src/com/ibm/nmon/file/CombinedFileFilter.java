package com.ibm.nmon.file;

import java.io.FileFilter;

import java.io.File;

import java.util.List;

/**
 * FileFilter that delegates to a list of {@link SwingAndIOFileFilter SwingAndIOFileFilters}.
 */
public final class CombinedFileFilter implements FileFilter {
    private final List<SwingAndIOFileFilter> filters = new java.util.ArrayList<SwingAndIOFileFilter>();

    @Override
    public boolean accept(File pathname) {
        // note first filter will accept a directory if needed
        // no need for an explicit check here
        for (SwingAndIOFileFilter filter : filters) {
            if (filter.accept(pathname)) {
                return true;
            }
        }

        return false;
    }

    public Iterable<SwingAndIOFileFilter> getFilters() {
        return java.util.Collections.unmodifiableList(filters);
    }

    public NMONFileFilter getNMONFileFilter() {
        return (NMONFileFilter) filters.get(0).getFilter();
    }

    public GCFileFilter getGCFileFilter() {
        return (GCFileFilter) filters.get(1).getFilter();
    }

    public IOStatFileFilter getIOStatFileFilter() {
        return (IOStatFileFilter) filters.get(2).getFilter();
    }

    public JSONFileFilter getJSONFileFilter() {
        return (JSONFileFilter) filters.get(3).getFilter();
    }

    public HATJFileFilter getHATJFileFilter() {
        return (HATJFileFilter) filters.get(4).getFilter();
    }

    public PerfmonFileFilter getPerfmonFileFilter() {
        return (PerfmonFileFilter) filters.get(5).getFilter();
    }

    private CombinedFileFilter(boolean acceptDirectories) {
        filters.add(new SwingAndIOFileFilter("NMON Files", new NMONFileFilter(), acceptDirectories));
        filters.add(new SwingAndIOFileFilter("Verbose GC Logs", new GCFileFilter(), acceptDirectories));
        filters.add(new SwingAndIOFileFilter("IOStat Files", new IOStatFileFilter(), acceptDirectories));
        filters.add(new SwingAndIOFileFilter("JSON Files", new JSONFileFilter(), acceptDirectories));
        filters.add(new SwingAndIOFileFilter("HATJ CSV Files", new HATJFileFilter(), acceptDirectories));
        filters.add(new SwingAndIOFileFilter("Perfmon CSV Files", new PerfmonFileFilter(), acceptDirectories));
    }

    private static final CombinedFileFilter INSTANCE = new CombinedFileFilter(false);
    private static final CombinedFileFilter INSTANCE_WITH_DIRECTORIES = new CombinedFileFilter(true);

    public static CombinedFileFilter getInstance(boolean acceptDirectories) {
        if (acceptDirectories) {
            return INSTANCE_WITH_DIRECTORIES;
        }
        else {
            return INSTANCE;
        }
    }
}
