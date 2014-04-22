package com.ibm.nmon.gui.table;

import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Component;

import javax.swing.JTable;

import com.ibm.nmon.gui.Styles;

/**
 * Renders double data with thousands separator and 3 decimal points. Renders NaN as "N/A".
 */
public final class DoubleCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -8018894006090877953L;

    public DoubleCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);

        setHorizontalAlignment(TRAILING);
        formatDouble(value);

        return this;
    }

    private void formatDouble(Object value) {
        double d = (Double) value;

        if (Double.isNaN(d)) {
            setValue("N/A");
        }
        else {
            setValue(Styles.NUMBER_FORMAT.format(d));
        }
    }
}
