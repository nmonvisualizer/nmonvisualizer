package com.ibm.nmon.gui.file;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;

import java.io.IOException;

import com.ibm.nmon.analysis.AnalysisSet;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

import com.ibm.nmon.data.ProcessDataSet;
import com.ibm.nmon.data.ProcessDataType;
import com.ibm.nmon.data.Process;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * JFileChooser for selecting the location to save AnalysisSet files. Also responsible for parsing
 * the files and adding them to the given {@link AnalysisSet}.
 */
public final class AnalysisSetFileChooser extends GUIFileChooser {
    private final AnalysisSet analysis;

    public AnalysisSetFileChooser(NMONVisualizerGui gui, AnalysisSet analysis) {
        super(gui, "Select Table Definition File", "analysis.properties");

        this.analysis = analysis;
    }

    public void save() {
        if (showDialog(gui.getMainFrame(), "Save") == JFileChooser.APPROVE_OPTION) {
            File analysisFile = getSelectedFile();

            if (analysisFile.exists()) {
                int result = JOptionPane.showConfirmDialog(gui.getMainFrame(), "File '" + analysisFile.getName()
                        + "' already exists.\nDo you want to overwrite it?", "Overwrite?",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            FileWriter writer = null;

            try {
                writer = new FileWriter(analysisFile);

                for (String key : analysis.getKeys()) {
                    DataType type = analysis.getType(key);

                    if (type instanceof ProcessDataType) {
                        writer.write(key);
                        writer.write(':');
                        writer.write(((ProcessDataType) type).getProcess().getCommandLine());
                        writer.write('\n');
                    }
                    else {
                        writer.write(key);
                        writer.write('\n');
                    }
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
            File analysisFile = getSelectedFile();

            if (!analysisFile.exists()) {
                int result = JOptionPane.showConfirmDialog(gui.getMainFrame(), "File '" + analysisFile.getName()
                        + "' is not a valid file", "Invalid File", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.ERROR_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new java.io.FileReader(analysisFile));

                String line = null;

                while ((line = reader.readLine()) != null) {
                    // assume : is the key separator
                    int idx = line.indexOf(':');

                    if (idx == -1) {
                        continue;
                    }

                    String typeId = line.substring(0, idx);
                    String field = line.substring(idx + 1);
                    String command = null;

                    idx = line.indexOf(':', idx + 1);

                    if (idx != -1) {
                        field = line.substring(typeId.length() + 1, idx);
                        command = line.substring(idx + 1);
                    }

                    for (DataSet data : gui.getDataSets()) {
                        if (command == null) {
                            DataType type = data.getType(typeId);

                            if ((type != null) && type.hasField(field)) {
                                analysis.addData(type, field);
                            }
                        }
                        else if (data instanceof ProcessDataSet) {
                            ProcessDataSet processData = (ProcessDataSet) data;

                            // add all Processes with the same command line
                            // this allows the process id to change but the analysis set to always
                            // show the same data
                            for (Process process : processData.getProcesses()) {
                                if (process.getCommandLine().equals(command)) {
                                    analysis.addData(processData.getType(process), field);
                                }
                            }
                        }
                    }
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
