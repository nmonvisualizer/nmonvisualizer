package com.ibm.nmon.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.JTable;

import javax.swing.TransferHandler;

import javax.swing.table.TableModel;

import com.ibm.nmon.data.DataTuple;
import com.ibm.nmon.gui.analysis.ByDataSetTableModel;
import com.ibm.nmon.gui.analysis.ByStatisticTableModel;

import com.ibm.nmon.analysis.AnalysisSet;

/**
 * Support dragging data to a table from a tree. Also, support copying table data to the clipboard.
 */
public final class TableTransferHandler extends TransferHandler {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.000");

    private final JTable table;
    private final AnalysisSet analysisSet;

    public TableTransferHandler(JTable table, AnalysisSet analysisSet) {
        this.table = table;
        this.analysisSet = analysisSet;
    }

    @Override
    public boolean importData(TransferSupport support) {
        Object data = null;

        try {
            data = support.getTransferable().getTransferData(DataTransferable.FLAVORS[0]);
        }
        catch (Exception e) {
            return false;
        }

        if (data.getClass() != DataTransferable.class) {
            return false;
        }

        DataTransferable transferable = (DataTransferable) data;

        for (DataTuple tuple : transferable.getTuples()) {
            if (tuple.getField() == null) {
                analysisSet.addData(tuple.getDataType());
            }
            else {
                analysisSet.addData(tuple.getDataType(), tuple.getField());
            }
        }

        return true;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        boolean supportedFlavor = false;

        for (DataFlavor flavor : support.getDataFlavors()) {
            if (flavor.isMimeTypeEqual(DataTransferable.FLAVORS[0])) {
                supportedFlavor = true;
                break;
            }
        }

        return supportedFlavor;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    // note this also handles copy (CTRL-C) from the table
    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c != table) {
            return null;
        }

        return copyTable(false);
    }

    public Transferable copyAll() {
        return copyTable(true);
    }

    private Transferable copyTable(boolean allRows) {
        int columnStartIdx = 0;

        TableModel model = table.getModel();

        if (model instanceof ByDataSetTableModel) {
            // skip metric name
            columnStartIdx = 1;
        }
        else if (model instanceof ByStatisticTableModel) {
            columnStartIdx = 0;
        }
        else {
            return null;
        }

        StringBuilder builder = new StringBuilder(1024);

        int rows[] = null;
        int n = 0;

        if (allRows) {
            rows = null;
            n = table.getRowCount();
        }
        else {
            rows = table.getSelectedRows();
            n = rows.length;
        }

        for (int i = 0; i < n; i++) {
            int row = allRows ? i : rows[i];

            for (int j = columnStartIdx; j < table.getColumnCount(); j++) {
                Object o = table.getValueAt(row, j);

                if (o instanceof Double) {
                    double d = (Double) o;

                    if (!Double.isNaN(d)) {
                        builder.append(FORMAT.format(d));
                    }
                }
                else {
                    builder.append(o.toString());
                }

                builder.append(',');
            }

            // remove trailing ','
            builder.deleteCharAt(builder.length() - 1);
            builder.append('\n');
        }

        return new StringSelection(builder.toString());
    }
}
