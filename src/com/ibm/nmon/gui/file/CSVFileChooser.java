package com.ibm.nmon.gui.file;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.List;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.util.CSVWriter;

/**
 * JFileChooser for selecting the location to save CSV files.
 * 
 * @see CSVWriter
 */
public final class CSVFileChooser extends GUIFileChooser {
    private static final long serialVersionUID = 6573073002190791008L;

    private final DataSet data;
    private final DataType type;
    private final List<String> fields;

    public CSVFileChooser(NMONVisualizerGui gui, DataSet data, DataType type, List<String> fields) {
        super(gui, "Select CSV Save Location", getFileName(data, type, fields));

        this.data = data;
        this.type = type;
        this.fields = fields;
    }

    private static String getFileName(DataSet data, DataType type, List<String> fields) {
        String typeName = "";

        if (type != null) {
            typeName = '_' + type.toString();
        }

        String fieldName = "";

        if ((fields != null) && fields.size() == 1) {
            fieldName = '-' + fields.get(0);
        }

        return data.getHostname() + typeName + fieldName + ".csv";
    }

    public void saveToCSV() {
        if (showDialog(gui.getMainFrame(), "Save") == JFileChooser.APPROVE_OPTION) {
            File csvFile = getSelectedFile();

            if (csvFile.exists()) {
                int result = JOptionPane.showConfirmDialog(gui.getMainFrame(), "File '" + csvFile.getName()
                        + "' already exists.\nDo you want to overwrite it?", "Overwrite?",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            BufferedWriter writer = null;

            try {
                writer = new BufferedWriter(new FileWriter(csvFile));

                if (type == null) {
                    // write the whole file
                    CSVWriter.write(data, gui.getIntervalManager().getCurrentInterval(), writer);
                }
                else {
                    CSVWriter.write(data, type, fields, gui.getIntervalManager().getCurrentInterval(), writer);
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
}
