package com.ibm.nmon.gui.chart.summary;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

final class ChartSummaryMenu extends MouseAdapter {
    private final ChartSummaryPanel summaryPanel;

    private final JPopupMenu menuForRows;
    private final JPopupMenu menuForNoRows;
    private final JPopupMenu menuForNoVisibilty;

    public ChartSummaryMenu(ChartSummaryPanel summaryTable) {
        this.summaryPanel = summaryTable;

        ActionListener selectColumns = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ChartSummaryMenu.this.summaryPanel.displayTableColumnChooser();
            }
        };

        ActionListener showNone = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ChartSummaryTableModel tableModel = ChartSummaryMenu.this.summaryPanel.getTableModel();

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(false, i, 0);
                }

                ChartSummaryMenu.this.summaryPanel.repaint();
            }
        };

        ActionListener showAll = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ChartSummaryTableModel tableModel = ChartSummaryMenu.this.summaryPanel.getTableModel();

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(true, i, 0);
                }

                ChartSummaryMenu.this.summaryPanel.repaint();
            }
        };

        // menu displayed when rows are selected
        menuForRows = new JPopupMenu();
        // menu displayed when no rows are selected
        menuForNoRows = new JPopupMenu();
        // menu displayed on charts where visibility changes are not enabled
        menuForNoVisibilty = new JPopupMenu();

        JMenuItem item = new JMenuItem("Show Selected Only");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ChartSummaryTableModel tableModel = ChartSummaryMenu.this.summaryPanel.getTableModel();
                int[] selectedRows = ChartSummaryMenu.this.summaryPanel.getSummaryTable().getSelectedRows();

                for (int i = 0; i < selectedRows.length; i++) {
                    selectedRows[i] = ChartSummaryMenu.this.summaryPanel.getSummaryTable().convertRowIndexToModel(
                            selectedRows[i]);
                }

                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    boolean selected = false;

                    for (int j = 0; j < selectedRows.length; j++) {
                        if (i == selectedRows[j]) {
                            tableModel.setValueAt(true, i, 0);
                            selected = true;
                            break;
                        }
                    }

                    if (!selected) {
                        tableModel.setValueAt(false, i, 0);
                    }
                }

                ChartSummaryMenu.this.summaryPanel.repaint();
            }
        });
        menuForRows.add(item);

        // show select none and select all in all cases
        // items can only be added to a single menu; create copies for each
        item = new JMenuItem("Show None");
        item.addActionListener(showNone);
        menuForRows.add(item);

        item = new JMenuItem("Show None");
        item.addActionListener(showNone);
        menuForNoRows.add(item);

        item = new JMenuItem("Show All");
        item.addActionListener(showAll);
        menuForRows.add(item);

        item = new JMenuItem("Show All");
        item.addActionListener(showAll);
        menuForNoRows.add(item);

        menuForRows.addSeparator();
        menuForNoRows.addSeparator();

        item = new JMenuItem("Select Columns...");
        item.addActionListener(selectColumns);
        menuForRows.add(item);

        item = new JMenuItem("Select Columns...");
        item.addActionListener(selectColumns);
        menuForNoRows.add(item);

        item = new JMenuItem("Select Columns...");
        item.addActionListener(selectColumns);
        menuForNoVisibilty.add(item);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseReleased(e);
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        // popup menu on right click
        if (e.isPopupTrigger()) {
            if (summaryPanel.getTableModel().getEnabled(ChartSummaryTableModel.VISIBLE)) {
                int row = summaryPanel.getSummaryTable().rowAtPoint(e.getPoint());
                int[] selectedRows = summaryPanel.getSummaryTable().getSelectedRows();

                // if rows are selected, always show the Selectxx items
                // if no rows are selected and the click is on a row,
                // select it then show the Selectxx items
                if (row < 0) {
                    if (selectedRows.length == 0) {
                        menuForNoRows.show(e.getComponent(), e.getX(), e.getY());
                    }
                    else {
                        menuForRows.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
                else {
                    if (selectedRows.length == 0) {
                        summaryPanel.getSummaryTable().changeSelection(row, 0, false, false);
                        menuForRows.show(e.getComponent(), e.getX(), e.getY());
                    }

                    else {
                        menuForRows.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
            else {
                menuForNoVisibilty.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
