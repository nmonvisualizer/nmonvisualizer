package com.ibm.nmon.gui.parse;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Dialog to get the hostname for a particular ZPool IOStatFile file.
 * 
 * @see com.ibm.nmon.gui.main.NMONVisualizerGui#get
 */
public final class ZPoolIOStatPostParser extends BaseParserDialog {
    private static final long serialVersionUID = -1327608292470726453L;

    public ZPoolIOStatPostParser(NMONVisualizerGui gui) {
        super(gui, "Missing ZPool Information");
    }
}
