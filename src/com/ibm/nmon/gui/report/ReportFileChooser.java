package com.ibm.nmon.gui.report;

import com.ibm.nmon.gui.file.GUIFileChooser;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

final class ReportFileChooser extends GUIFileChooser {

    ReportFileChooser(NMONVisualizerGui gui) {
        super(gui, "Parse Report XML");
    }
}
