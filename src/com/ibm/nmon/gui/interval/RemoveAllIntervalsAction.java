package com.ibm.nmon.gui.interval;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

import java.awt.Component;

import javax.swing.JOptionPane;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Action listener that removes all intervals from the IntervalManager.
 */
public final class RemoveAllIntervalsAction extends AbstractAction {
    private final NMONVisualizerGui gui;
    private final Component parent;

    public RemoveAllIntervalsAction(NMONVisualizerGui gui, Component parent) {
        this.gui = gui;
        this.parent = parent;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (JOptionPane.showConfirmDialog(parent, "Are you sure?", "Remove All Intervals", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
            gui.getIntervalManager().clearIntervals();
        }
    }
}
