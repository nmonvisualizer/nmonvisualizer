package com.ibm.nmon.file;

import java.io.FileFilter;
import java.io.File;

abstract class BaseFileFilter implements FileFilter {
    @Override
    public final boolean accept(File pathname) {
        return accept(pathname.getName());
    }

    public abstract boolean accept(String pathname);
}
