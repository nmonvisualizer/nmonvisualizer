package com.ibm.nmon.gui.dnd;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

import java.awt.datatransfer.Transferable;

import java.io.File;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataTuple;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.Process;
import com.ibm.nmon.data.ProcessDataSet;
import com.ibm.nmon.data.ProcessDataType;

import com.ibm.nmon.file.CombinedFileFilter;
import com.ibm.nmon.gui.file.ParserRunner;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.tree.TreePathParser;
import com.ibm.nmon.util.FileHelper;

/**
 * Support dragging data from a tree. Does not support exporting / copying tree data. Currently only
 * supports DataTypes and fields; entire DataSets cannot be dragged.
 */
public final class TreeTransferHandler extends TransferHandler {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TreeTransferHandler.class);

    private final TreeTransferPathParser pathParser;
    private final NMONVisualizerGui gui;

    public TreeTransferHandler(NMONVisualizerGui gui) {
        super();

        pathParser = new TreeTransferPathParser(gui);
        this.gui = gui;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (!c.getClass().equals(JTree.class)) {
            return null;
        }

        TreePath[] paths = ((JTree) c).getSelectionPaths();

        if (paths.length != 0) {

            try {
                for (TreePath path : paths) {
                    pathParser.parse(path);
                }

                return pathParser.getTransferable();
            }
            finally {
                // prevent a memory leak since the transferable is holding a reference to the whole
                // data set
                pathParser.clearTransferable();
            }
        }
        else {
            return null;
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        // disable
        return;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        DataFlavor[] flavors = support.getDataFlavors();

        for (DataFlavor flavor : flavors) {
            if (flavor.isFlavorJavaFileListType()) {
                return true;
            }
        }

        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferSupport support) {
        try {
            List<java.io.File> files = (List<File>) support.getTransferable().getTransferData(
                    DataFlavor.javaFileListFlavor);

            List<String> toParse = new java.util.ArrayList<String>(files.size());

            FileHelper.recurseDirectories(files, CombinedFileFilter.getInstance(false), toParse);

            new Thread(new ParserRunner(gui, toParse, gui.getDisplayTimeZone()), "DataSet Parser").start();

            return true;
        }
        catch (Exception e) {
            LOGGER.warn("cannot import data", e);
            return false;
        }
    }

    private static final class TreeTransferPathParser extends TreePathParser {
        private NMONVisualizerGui gui;
        private DataTransferable transferable;

        public TreeTransferPathParser(NMONVisualizerGui gui) {
            this.gui = gui;
        }

        protected void onTypePath(DataSet dataSet, DataType type) {
            if (transferable == null) {
                transferable = new DataTransferable();
            }

            // optimization if adding the whole Type, leave the field null and add 1 tuple
            // this will allow the importers to fire less events / do less work
            matchByCommandLine(dataSet, type, null);
        }

        protected void onFieldPath(DataSet dataSet, DataType type, String field) {
            if (transferable == null) {
                transferable = new DataTransferable();
            }

            matchByCommandLine(dataSet, type, field);
        }

        public DataTransferable getTransferable() {
            return transferable;
        }

        public void clearTransferable() {
            transferable = null;
        }

        private void matchByCommandLine(DataSet dataSet, DataType type, String field) {
            // for process types, add all types with the same command line
            if (type instanceof ProcessDataType) {
                String command = ((ProcessDataType) type).getProcess().getCommandLine();

                for (DataSet data : gui.getDataSets()) {
                    if (data instanceof ProcessDataSet) {
                        ProcessDataSet processData = (ProcessDataSet) data;

                        for (Process process : processData.getProcesses()) {
                            if (process.getCommandLine().equals(command)) {
                                transferable.addTuple(new DataTuple(processData, processData.getType(process), field));
                            }
                        }
                    }
                }
            }
            else {
                transferable.addTuple(new DataTuple(dataSet, type, field));
            }
        }
    };
}
