package com.ibm.nmon.gui.table;

import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Component;

import java.text.DecimalFormat;

import javax.swing.JTable;

/**
 * Renders integer data with thousands separator.
 */
public final class IntegerCellRenderer extends DefaultTableCellRenderer {
    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0");

    public IntegerCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);

        setHorizontalAlignment(TRAILING);
        setValue(FORMAT.format(value));

        return this;
    }
}
