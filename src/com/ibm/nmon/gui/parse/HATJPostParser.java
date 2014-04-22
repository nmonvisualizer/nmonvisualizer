package com.ibm.nmon.gui.parse;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Dialog to get the hostname for a particular HATJ CSV file.
 * 
 * @see com.ibm.nmon.gui.main.NMONVisualizerGui#getDataForHATJParse(String)
 */
public final class HATJPostParser extends BaseParserDialog {
    private static final long serialVersionUID = -1327608292470726493L;

    public HATJPostParser(NMONVisualizerGui gui) {
        super(gui, "Missing HATJ Information");
    }
}
