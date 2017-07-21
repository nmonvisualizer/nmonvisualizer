package com.ibm.nmon.gui.util;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JLabel;

import javax.swing.BorderFactory;

import com.ibm.nmon.gui.Styles;

/**
 * Modal progress dialog box that includes a name for the item and a progress bar.
 */
public final class ItemProgressDialog extends JDialog {
    private static final long serialVersionUID = -7316137795169235117L;

    private final JFrame parent;
    private final JLabel itemName;
    private final JProgressBar progress;

    private final int totalItems;
    private int itemCount;

    // label for initial, empty item name
    // allows layout to be calculated so that pack() calls (and resizing) is avoided as items are
    // updated
    private static final String DUMMY = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public ItemProgressDialog(JFrame parent, String title, int totalItems) {
        super(parent, title, true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        this.parent = parent;
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
            java.awt.Point location = parent.getLocation();

            setLocation(location.x + parent.getWidth() / 2 - getWidth() / 2,
                    location.y + parent.getHeight() / 2 - (getHeight() * 2));

        }

        super.setVisible(b);
    }

    public void setCurrentItem(String name) {
        itemName.setText(name);
    }

    public void updateProgress() {
        progress.setValue(++itemCount);
        progress.setString(itemCount + " / " + totalItems);
    }
}
