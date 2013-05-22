package com.ibm.nmon.gui.util;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JLabel;

import javax.swing.BorderFactory;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

public final class ItemProgressDialog extends JDialog {
    private final NMONVisualizerGui gui;
    private final JLabel itemName;
    private final JProgressBar progress;

    private final int totalItems;
    private int itemCount;

    // label for initial, empty item name
    // allows layout to be calculated so that pack() calls (and resizing) is avoided as items are
    // updated
    private static final String DUMMY = new String(new byte[40]);

    public ItemProgressDialog(NMONVisualizerGui gui, String title, int totalItems) {
        // dialog with the name of the item being processed on top and progress bar below
        super(gui.getMainFrame(), title, true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        this.gui = gui;
        this.totalItems = totalItems;
        itemCount = 0;

        progress = new JProgressBar(0, totalItems);
        progress.setStringPainted(true);
        progress.setFont(Styles.LABEL);
        progress.setString(itemCount + " / " + totalItems);
        progress.setValue(itemCount);

        itemName = new JLabel(DUMMY);
        itemName.setFont(Styles.TITLE);
        itemName.setBorder(BorderFactory.createEmptyBorder(3, 5, 5, 5));

        add(progress, java.awt.BorderLayout.CENTER);
        add(itemName, java.awt.BorderLayout.PAGE_START);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            pack();

            // centered in the X, a little bit higher in the Y
            java.awt.Point location = gui.getMainFrame().getLocation();

            setLocation(location.x + gui.getMainFrame().getWidth() / 2 - getWidth() / 2, location.y
                    + gui.getMainFrame().getHeight() / 2 - (getHeight() * 2));

        }

        super.setVisible(b);
    }

    public void setCurrentItem(String name) {
        itemName.setText(name);
        // pack();
    }

    public void updateProgress() {
        progress.setValue(++itemCount);
        progress.setString(itemCount + " / " + totalItems);
    }
}
