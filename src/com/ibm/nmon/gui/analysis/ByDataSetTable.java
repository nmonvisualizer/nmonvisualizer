package com.ibm.nmon.gui.analysis;

import java.awt.event.MouseEvent;

import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;

import com.ibm.nmon.gui.GUITable;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

final class ByDataSetTable extends GUITable {
    private static final long serialVersionUID = -7024288344925891200L;

    public ByDataSetTable(NMONVisualizerGui gui) {
        super(gui);

        ToolTipManager.sharedInstance().registerComponent(this.getTableHeader());
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(getColumnModel()) {
            private static final long serialVersionUID = -9130260383688373828L;

            @Override
            public String getToolTipText(MouseEvent event) {
                super.getToolTipText(event);

                int column = getTable().convertColumnIndexToModel(
                        ((DefaultTableColumnModel) getTable().getColumnModel()).getColumnIndexAtX(event.getX()));

                // skip tooltips on Data Type and Metric columns
                if (column > 1) {
                    return ((ByDataSetTableModel) getTable().getModel()).getColumnName(column);
                }
                else {
                    return null;
                }
            }
        };
    }
}