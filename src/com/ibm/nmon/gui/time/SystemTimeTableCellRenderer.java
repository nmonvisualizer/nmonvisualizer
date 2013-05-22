package com.ibm.nmon.gui.time;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import javax.swing.table.DefaultTableCellRenderer;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.util.TimeFormatCache;

/**
 * Renders system start and end times based on the current interval. If the interval overlaps a
 * given time, it will be bold. If the interval exactly aligns with a time, it will be bold and
 * italic. If an interval does not include a time, the text will be striken out.
 */
final class SystemTimeTableCellRenderer extends DefaultTableCellRenderer {
    private Interval toCompare;

    public SystemTimeTableCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // based on columns in SystemTimeTableModel
        if (column == 0) {
            setHorizontalAlignment(SwingConstants.TRAILING);
        }
        else if (column == 1) {
            long start = (Long) value;
            long intervalStart = toCompare.getStart();

            if (start == intervalStart) {
                setFont(Styles.BOLD_ITALIC);
            }
            else if (intervalStart < start) {
                if (toCompare.getEnd() < start) {
                    setFont(Styles.STRIKETHROUGH);
                }
                else {
                    setFont(Styles.BOLD);
                }
            }
            else {
                long end = (Long) table.getValueAt(row, column + 1);

                if (intervalStart > end) {
                    setFont(Styles.STRIKETHROUGH);
                }
                // else leave unformatted
            }

            setText(TimeFormatCache.formatDateTime(start));
        }
        else if (column == 2) {
            long end = (Long) value;
            long intervalEnd = toCompare.getEnd();

            if (end == intervalEnd) {
                setFont(Styles.BOLD_ITALIC);
            }
            else if (intervalEnd > end) {
                if (toCompare.getStart() > end) {
                    setFont(Styles.STRIKETHROUGH);
                }
                else {
                    setFont(Styles.BOLD);
                }
            }
            else {
                long start = (Long) table.getValueAt(row, column - 1);

                if (intervalEnd < start) {
                    setFont(Styles.STRIKETHROUGH);
                }
                // else leave unformatted
            }

            setText(TimeFormatCache.formatDateTime(end));
        }
        else if (column == 3) {
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        // else just return the value

        return this;
    }

    public void setIntervalToCompare(Interval toCompare) {
        this.toCompare = toCompare;
    }
}
