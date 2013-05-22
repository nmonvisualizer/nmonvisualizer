package com.ibm.nmon.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.io.IOException;

import java.util.List;

import com.ibm.nmon.data.DataTuple;

/**
 * Custom drag and drop object that contains the {@link DataTuple DataTuples} that are being
 * dragged.
 */
final class DataTransferable implements Transferable {
    static final DataFlavor[] FLAVORS;

    // only support inter-JVM dragging
    static {
        DataFlavor temp = null;

        try {
            temp = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        FLAVORS = new DataFlavor[] { temp };
    }

    private final List<DataTuple> toTransfer = new java.util.ArrayList<DataTuple>(2);

    DataTransferable() {}

    void addTuple(DataTuple tuple) {
        toTransfer.add(tuple);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType)) {
            return this;
        }
        else {
            return null;
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType);
    }

    Iterable<DataTuple> getTuples() {
        return toTransfer;
    }
}
