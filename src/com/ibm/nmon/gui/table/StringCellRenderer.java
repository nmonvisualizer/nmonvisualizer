package com.ibm.nmon.gui.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.ibm.nmon.gui.Styles;

/**
 * Makes string columns bold and right aligned.
 */
public final class StringCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 7643288582532589280L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        setHorizontalAlignment(TRAILING);
        setFont(Styles.BOLD);

        return this;
    }
}
