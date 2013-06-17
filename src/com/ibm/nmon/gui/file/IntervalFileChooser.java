package com.ibm.nmon.gui.file;

import org.slf4j.Logger;

import javax.swing.BorderFactory;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.io.File;

import java.io.IOException;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * JFileChooser for selecting the location to save Interval files. Also responsible for parsing
 * interval files and adding them to the gui's {@link IntervalManager}.
 */
public final class IntervalFileChooser extends GUIFileChooser {
    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(IntervalFileChooser.class);

    private JCheckBox useRelativeTime;

    public IntervalFileChooser(NMONVisualizerGui gui) {
        super(gui, "Select Interval File", "intervals.properties");

        useRelativeTime = new JCheckBox();
        useRelativeTime.setText("Relative Time?");
        useRelativeTime.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        useRelativeTime.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        useRelativeTime.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        addComponentToChooser(this, null, useRelativeTime);
    }

    public void save() {
        if (showDialog(gui.getMainFrame(), "Save") == JFileChooser.APPROVE_OPTION) {
            File intervalFile = getSelectedFile();

            if (intervalFile.exists()) {
                int result = JOptionPane.showConfirmDialog(gui.getMainFrame(), "File \"" + intervalFile.getName()
                        + "\" already exists.\nDo you want to overwrite it?", "Overwrite?",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            try {
                long offset = 0;

                if (useRelativeTime.isSelected()) {
                    offset = gui.getMinSystemTime();
                }

                gui.getIntervalManager().saveToFile(intervalFile, offset);
            }
            catch (IOException ioe) {
                logger.error("could not load interval file '{}'", intervalFile.getAbsolutePath(), ioe);
            }

        }
    }

    public void load() {
        if (showDialog(gui.getMainFrame(), "Load") == JFileChooser.APPROVE_OPTION) {
            File intervalFile = getSelectedFile();

            if (!intervalFile.exists()) {
                int result = JOptionPane.showConfirmDialog(gui.getMainFrame(), "File \"" + intervalFile.getName()
                        + "\" is not a valid file", "Invalid File", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.ERROR_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            try {
                long offset = 0;

                if (useRelativeTime.isSelected()) {
                    offset = gui.getMinSystemTime();
                }

                gui.getIntervalManager().loadFromFile(intervalFile, offset);
            }
            catch (IOException ioe) {
                logger.error("could not save interval file '{}'", intervalFile.getAbsolutePath(), ioe);
            }
        }
    }
}
