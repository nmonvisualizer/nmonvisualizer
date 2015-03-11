package com.ibm.nmon.util;

import java.io.File;
import java.io.FileFilter;

import java.util.List;

/**
 * Utility methods for recursive directory searches.
 */
public final class FileHelper {
    public static void recurseDirectories(File[] files, FileFilter filter, List<String> filenames) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                recurseDirectories(files[i].listFiles(), filter, filenames);
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
                recurseDirectories(file.listFiles(), filter, filenames);
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
