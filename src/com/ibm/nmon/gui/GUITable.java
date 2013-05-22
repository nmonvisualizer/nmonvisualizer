package com.ibm.nmon.gui;

import javax.swing.JTable;
import javax.swing.ToolTipManager;

import javax.swing.table.TableModel;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Defines an {@link NMONVisualizerGui gui} aware table with a common look and feel.
 */
public class GUITable extends JTable {
    protected final NMONVisualizerGui gui;

    public GUITable(NMONVisualizerGui gui) {
        super();

        this.gui = gui;

        setup();
    }

    public GUITable(NMONVisualizerGui gui, TableModel dm) {
        super(dm);

        this.gui = gui;

        setup();
    }

    private void setup() {
        setFillsViewportHeight(true);
        setRowHeight(20);

        setAutoCreateRowSorter(true);

        getTableHeader().setFont(Styles.BOLD);

        // keeps table from constantly calling the table cell renderer
        ToolTipManager.sharedInstance().unregisterComponent(this);
        ToolTipManager.sharedInstance().unregisterComponent(this.getTableHeader());
    }

    /**
     * Override to only call <code>super.clearSelection()</code> when there is actually a row
     * selected.
     */
    @Override
    public void clearSelection() {
        if (getSelectedRow() != -1) {
            super.clearSelection();
        }
    }
}
