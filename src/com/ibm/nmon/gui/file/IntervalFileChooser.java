package com.ibm.nmon.gui.file;

import javax.swing.BorderFactory;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;

import java.io.IOException;

import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.interval.IntervalManager;
import com.ibm.nmon.util.DataHelper;

/**
 * JFileChooser for selecting the location to save Interval files. Also responsible for parsing
 * interval files and adding them to the gui's {@link IntervalManager}.
 */
public final class IntervalFileChooser extends GUIFileChooser {
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

            FileWriter writer = null;

            try {
                writer = new FileWriter(intervalFile);

                for (Interval interval : gui.getIntervalManager().getIntervals()) {
                    long start = interval.getStart();
                    long end = interval.getEnd();

                    if (useRelativeTime.isSelected()) {
                        start -= gui.getMinSystemTime();
                        end -= gui.getMinSystemTime();
                    }

                    writer.write(interval.getName());
                    writer.write(':');
                    writer.write(Long.toString(start));
                    writer.write(':');
                    writer.write(Long.toString(end));
                    writer.write('\n');
                }
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
            finally {
                if (writer != null) {
                    try {
                        writer.close();
                    }
                    catch (IOException ioe2) {
                        // ignore
                    }
                }
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

            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new java.io.FileReader(intervalFile));

                String line = null;
                Interval interval = null;

                while ((line = reader.readLine()) != null) {
                    int idx1 = line.indexOf(':');

                    if (idx1 == -1) {
                        continue;
                    }

                    int idx2 = line.indexOf(':', idx1 + 1);

                    String name = null;
                    long start = 0;
                    long end = 0;

                    // may or may not have a name
                    if (idx2 == -1) {
                        start = Long.parseLong(line.substring(0, idx1));
                        end = Long.parseLong(line.substring(idx1 + 1));
                    }
                    else {
                        name = DataHelper.newString(line.substring(0, idx1));
                        start = Long.parseLong(line.substring(idx1 + 1, idx2));
                        end = Long.parseLong(line.substring(idx2 + 1));
                    }

                    if (useRelativeTime.isSelected()) {
                        start += gui.getMinSystemTime();
                        end += gui.getMinSystemTime();
                    }

                    interval = new Interval(start, end);
                    interval.setName(name);

                    gui.getIntervalManager().addInterval(interval);
                }

                if (interval != null) {
                    gui.getIntervalManager().setCurrentInterval(interval);
                }
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
            finally {
                if (reader != null) {
                    try {
                        reader.close();
                    }
                    catch (IOException ioe2) {
                        // ignore
                    }
                }
            }
        }
    }
}
