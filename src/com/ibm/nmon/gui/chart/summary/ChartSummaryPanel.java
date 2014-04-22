package com.ibm.nmon.gui.chart.summary;

import java.awt.Component;

import java.awt.event.MouseEvent;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;

import javax.swing.ToolTipManager;

import com.ibm.nmon.gui.GUITable;

import com.ibm.nmon.gui.chart.data.DataTupleDataset;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.table.DoubleCellRenderer;
import com.ibm.nmon.gui.table.IntegerCellRenderer;
import com.ibm.nmon.gui.table.StringCellRenderer;
import com.ibm.nmon.gui.table.TableColumnChooser;
import com.ibm.nmon.gui.util.ScrollingTableFix;

/**
 * Holder JPanel for the summary table displayed under charts.
 */
public final class ChartSummaryPanel extends JScrollPane implements PropertyChangeListener {
    private static final long serialVersionUID = -7959432908033304478L;

    private final NMONVisualizerGui gui;
    private final JFrame parent;

    private final GUITable summaryTable;
    private final ChartSummaryTableModel tableModel;

    public ChartSummaryPanel(NMONVisualizerGui gui, JFrame parent, String[] defaultColumns) {
        super();

        this.gui = gui;
        this.parent = parent;

        tableModel = new ChartSummaryTableModel(gui, defaultColumns);
        tableModel.addPropertyChangeListener(this);

        summaryTable = createTable();

        setViewportView(summaryTable);

        getViewport().setBackground(java.awt.Color.WHITE);
        addComponentListener(new ScrollingTableFix(summaryTable, this));

        // make sure panel takes up entire parent
        setBorder(null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("chart".equals(evt.getPropertyName())) {
            DataTupleDataset dataset = (DataTupleDataset) evt.getNewValue();

            if (evt.getNewValue() != null) {
                tableModel.setData(dataset);
            }
            else {
                tableModel.clear();
            }
        }
        else if ("rowVisible".equals(evt.getPropertyName())) {
            firePropertyChange("rowVisible", null, evt.getNewValue());
        }
    }

    private GUITable createTable() {
        GUITable table = new GUITable(gui, tableModel) {
            private static final long serialVersionUID = 2380417479703405189L;

            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(getColumnModel()) {
                    private static final long serialVersionUID = -4028332367434358857L;

                    @Override
                    public String getToolTipText(MouseEvent event) {
                        super.getToolTipText(event);

                        int column = getTable()
                                .convertColumnIndexToModel(
                                        ((DefaultTableColumnModel) getTable().getColumnModel()).getColumnIndexAtX(event
                                                .getX()));

                        return getColumnName(column);
                    }
                };
            }
        };

        table.addMouseListener(new ChartSummaryMenu(this));

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    boolean[] selectedRows = new boolean[summaryTable.getRowCount()];
                    ListSelectionModel model = (ListSelectionModel) e.getSource();

                    for (int i = model.getMinSelectionIndex(); i <= model.getMaxSelectionIndex(); i++) {
                        if (model.isSelectedIndex(i)) {
                            selectedRows[summaryTable.convertRowIndexToModel(i)] = true;
                        }
                    }

                    firePropertyChange("selectedRows", null, selectedRows);
                }
            }
        });

        ToolTipManager.sharedInstance().registerComponent(table.getTableHeader());

        // checkbox for enable / disable
        table.getColumnModel().getColumn(0).setMaxWidth(25);

        table.setDefaultRenderer(Boolean.class, new CheckBoxCellRenderer());
        table.setDefaultRenderer(Double.class, new DoubleCellRenderer());
        table.setDefaultRenderer(Integer.class, new IntegerCellRenderer());
        table.setDefaultRenderer(String.class, new StringCellRenderer());

        return table;
    }

    public void clearSelection() {
        summaryTable.clearSelection();
    }

    /**
     * Select a row in the table based on a row from a chart dataset. This is meant to be used for
     * XYDatasets.
     */
    public void selectRow(int datasetRow) {
        int row = summaryTable.convertRowIndexToView(datasetRow);

        summaryTable.getSelectionModel().setSelectionInterval(row, row);
        summaryTable.scrollRectToVisible(summaryTable.getCellRect(row, 0, false));
    }

    /**
     * Select a row in the table based on a row and column from a chart dataset. This is meant to be
     * used for Category datasets.
     */
    public void selectRow(int datasetRow, int datasetColumn) {
        int row = summaryTable.convertRowIndexToView(tableModel.getTableRow(datasetRow, datasetColumn));

        summaryTable.getSelectionModel().setSelectionInterval(row, row);
        summaryTable.scrollRectToVisible(summaryTable.getCellRect(row, 0, false));
    }

    public int getDatasetRow(int tableRow) {
        return tableModel.getDatasetRow(tableRow);
    }

    public int getDatasetColumn(int tableRow) {
        return tableModel.getDatasetColumn(tableRow);
    }

    public void displayTableColumnChooser() {
        new TableColumnChooser(gui, parent, tableModel);
    }

    GUITable getSummaryTable() {
        return summaryTable;
    }

    ChartSummaryTableModel getTableModel() {
        return tableModel;
    }

    private final class CheckBoxCellRenderer extends JCheckBox implements TableCellRenderer {
        private static final long serialVersionUID = 8963589765399665908L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            setSelected((Boolean) value);

            // the default check box renderer does not use the correct, alternating colors
            // this code is modified from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6723524
            Component other = (JLabel) table.getDefaultRenderer(String.class).getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);
            java.awt.Color bg = other.getBackground();

            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            }
            else {
                setForeground(other.getForeground());
                setBackground(new java.awt.Color(bg.getRed(), bg.getGreen(), bg.getBlue()));
            }

            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            setOpaque(true);

            return this;
        }
    }
}
