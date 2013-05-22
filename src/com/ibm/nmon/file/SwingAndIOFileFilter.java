package com.ibm.nmon.file;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Swing file filter that delegates to a java.io FileFilter.
 */
public class SwingAndIOFileFilter extends FileFilter {
    private final String description;
    private final java.io.FileFilter filter;

    private final boolean acceptsDirectories;

    public SwingAndIOFileFilter(String descrption, java.io.FileFilter filter, boolean acceptsDirectories) {
        this.description = descrption;
        this.filter = filter;
        this.acceptsDirectories = acceptsDirectories;
    }

    @Override
    public final boolean accept(File f) {
        if (acceptsDirectories && f.isDirectory()) {
            return true;
        }
        else {
            return getFilter().accept(f);
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    public java.io.FileFilter getFilter() {
        return filter;
    }
}
