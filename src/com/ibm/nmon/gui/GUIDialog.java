package com.ibm.nmon.gui;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JRootPane;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Base dialog class that contains a reference to the parent application. This class also adds the
 * ability to close the dialog with the ESC key.
 */
public abstract class GUIDialog extends JDialog {
    private static final long serialVersionUID = 5717736163308834942L;

    protected final NMONVisualizerGui gui;

    public GUIDialog(NMONVisualizerGui gui, JFrame parent, String title) {
        super(parent, title);

        this.gui = gui;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    public GUIDialog(NMONVisualizerGui gui) {
        this(gui, gui.getMainFrame(), "");
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            pack();
            setLocationRelativeTo(gui.getMainFrame());
        }

        super.setVisible(b);
    }

    @Override
    protected JRootPane createRootPane() {
        JRootPane rootPane = super.createRootPane();

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "ESCAPE");

        rootPane.getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = 4478727588396940932L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        return rootPane;
    }
}
