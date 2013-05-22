package com.ibm.nmon.gui.file;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.BoxLayout;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * Base class for file selection dialogs. Supports the notion of a default file name that can be
 * used when the user has not entered a file name.
 */
public class GUIFileChooser extends JFileChooser {
    protected final NMONVisualizerGui gui;

    private String defaultFileName;

    public GUIFileChooser(NMONVisualizerGui gui, String title) {
        this(gui, title, null);
    }

    public GUIFileChooser(NMONVisualizerGui gui, String title, String filename) {
        super();

        this.gui = gui;
        this.defaultFileName = filename;

        setDialogTitle(title);

        // these must be set _before_ setting the selected file or the selection will be lost!
        setMultiSelectionEnabled(false);
        setFileSelectionMode(JFileChooser.FILES_ONLY);

        String directory = gui.getPreferences().get("lastSaveDirectory", null);

        if (filename != null) {
            setSelectedFile(new File(directory, defaultFileName));
        }
        else if (directory != null) {
            setCurrentDirectory(new File(directory));
        }
    }

    @Override
    public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
        int toReturn = super.showDialog(parent, approveButtonText);

        saveLastSaveLocation();

        return toReturn;
    }

    @Override
    public int showOpenDialog(Component parent) throws HeadlessException {
        int toReturn = super.showOpenDialog(parent);
        saveLastSaveLocation();

        return toReturn;
    }

    @Override
    public int showSaveDialog(Component parent) throws HeadlessException {
        int toReturn = super.showSaveDialog(parent);

        saveLastSaveLocation();

        return toReturn;
    }

    private final void saveLastSaveLocation() {
        if (getSelectedFile() != null) {
            File parent = getSelectedFile();

            if (!parent.isDirectory()) {
                parent = parent.getParentFile();
            }

            if (parent != null) {
                gui.getPreferences().put("lastSaveDirectory", parent.getAbsolutePath());
            }
        }
    }

    protected static void addComponentToChooser(JFileChooser chooser, String toLabel, JComponent toAdd) {
        // huge hack that only works with the Nimbus look and feel
        // get the bottom panel and add the new component after the file type drop down
        JPanel bottom = ((JPanel) chooser.getComponent(chooser.getComponentCount() - 1));

        JPanel newPanel = new JPanel();
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.LINE_AXIS));

        if (toLabel != null) {
            // use the formatting of the combo box label in the component being added
            // count - 1 => buttons
            // count - 2 => filter filter combo box
            JPanel filterPanel = (JPanel) bottom.getComponent(bottom.getComponentCount() - 2);
            // count - 1 => combo box
            // count - 2 => label
            JLabel filterLabel = (JLabel) filterPanel.getComponent(filterPanel.getComponentCount() - 2);

            JLabel label = new JLabel(toLabel);
            label.setFont(filterLabel.getFont());
            label.setBorder(filterLabel.getBorder());
            label.setPreferredSize(filterLabel.getPreferredSize());

            newPanel.add(label);
        }

        newPanel.add(toAdd);

        bottom.add(newPanel, bottom.getComponentCount() - 1);
    }
}
