package com.ibm.nmon.util;

import java.io.File;
import java.io.FileFilter;

import java.util.List;

import org.slf4j.Logger;

/**
 * Utility methods for recursive directory searches.
 */
public final class FileHelper {
    protected final static Logger logger = org.slf4j.LoggerFactory.getLogger(FileHelper.class);

    public static void recurseDirectories(File[] files, FileFilter filter, List<String> filenames) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                File[] children = files[i].listFiles();
                if (children != null) {
                    recurseDirectories(children, filter, filenames);
                } else {
                    logger.warn("Listing files for " + files[i].getAbsolutePath() + " unexpectedly return null");
                }
            }
            else {
                if (filter.accept(files[i])) {
                    filenames.add(files[i].getAbsolutePath());
                }
            }
        }
    }

    public static void recurseDirectories(List<File> files, FileFilter filter, List<String> filenames) {
        for (File file : files) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    recurseDirectories(children, filter, filenames);
                } else {
                    logger.warn("Listing files for " + file.getAbsolutePath() + " unexpectedly return null");
                }
            }
            else {
                if (filter.accept(file)) {
                    filenames.add(file.getAbsolutePath());
                }
            }
        }
    }

    private FileHelper() {}
}
