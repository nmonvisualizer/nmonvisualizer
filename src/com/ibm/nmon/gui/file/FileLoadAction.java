package com.ibm.nmon.gui.file;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JFileChooser;

import com.ibm.nmon.file.CombinedFileFilter;
import com.ibm.nmon.file.SwingAndIOFileFilter;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.util.TimeZoneComboBox;

import com.ibm.nmon.util.FileHelper;

import java.io.File;

import java.util.List;

/**
 * Creates a JFileChooser so the user can select files to parse. Delegates actual parsing to
 * {@link ParserRunner}. Directory selection is supported as is recursion into a directory
 * structure.
 */
public final class FileLoadAction implements ActionListener {
    private final JFileChooser chooser;
    private final NMONVisualizerGui gui;

    private final TimeZoneComboBox timeZones;

    public FileLoadAction(NMONVisualizerGui gui) {
        this.gui = gui;

        String directory = gui.getPreferences().get("lastDirectory", null);

        chooser = new JFileChooser(directory);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setDialogTitle("Select Files to Parse");

        for (SwingAndIOFileFilter filter : CombinedFileFilter.getInstance(true).getFilters()) {
            chooser.addChoosableFileFilter(filter);
        }

        chooser.setFileFilter(chooser.getAcceptAllFileFilter());

        timeZones = new TimeZoneComboBox(gui.getDisplayTimeZone());

        GUIFileChooser.addComponentToChooser(chooser, "Time Zone:", timeZones);

    }

    public void actionPerformed(ActionEvent event) {
        if (chooser.showDialog(gui.getMainFrame(), "Parse") != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File[] selectedFiles = chooser.getSelectedFiles();

        if (selectedFiles.length == 0) {
            return;
        }

        gui.getPreferences().put("lastDirectory", selectedFiles[0].getParentFile().getAbsolutePath());

        List<String> toParse = new java.util.ArrayList<String>(selectedFiles.length);

        java.io.FileFilter filter = null;

        // user selected All Files => select all files filtered by all filters
        // user selected a single file type => select all files filtered by a single filter
        if (chooser.getFileFilter() == chooser.getAcceptAllFileFilter()) {
            filter = CombinedFileFilter.getInstance(false);
        }
        else {
            filter = ((SwingAndIOFileFilter) chooser.getFileFilter()).getFilter();
        }

        FileHelper.recurseDirectories(selectedFiles, filter, toParse);

        if (!toParse.isEmpty()) {
            // parse files outside of the Swing event thread
            new Thread(new ParserRunner(gui, toParse, timeZones.getSelectedTimeZone()), getClass().getName()
                    + " Parser").start();
        }
    }
}
