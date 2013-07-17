package com.ibm.nmon.gui.report;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import com.ibm.nmon.gui.file.GUIFileChooser;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

final class ReportFileChooser extends GUIFileChooser {

    ReportFileChooser(NMONVisualizerGui gui) {
        super(gui, "Parse Report XML");

        setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "Report XML Files";
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
            }
        });
    }
}
