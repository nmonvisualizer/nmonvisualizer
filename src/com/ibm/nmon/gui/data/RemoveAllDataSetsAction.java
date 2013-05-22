package com.ibm.nmon.gui.data;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Simple wrapper action that delegates to {@link NMONVisualizerGui#clearDataSets()}.
 */
public class RemoveAllDataSetsAction implements ActionListener {
    private final NMONVisualizerGui gui;
    private final Component parent;

    public RemoveAllDataSetsAction(NMONVisualizerGui gui, Component parent) {
        this.gui = gui;
        this.parent = parent;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (JOptionPane.showConfirmDialog(parent, "Are you sure?", "Remove All Data", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
            gui.clearDataSets();
        }
    }
}
