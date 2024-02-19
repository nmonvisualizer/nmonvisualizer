package com.ibm.nmon.gui.table;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.AbstractAction;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.GUITable;
import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * <p>
 * JDialog for selecting which columns to display in a {@link ChoosableColumnTableModel}.
 * </p>
 * 
 * <p>
 * This dialog creates a list containing the names of the columns from the model along with a
 * checkbox to select if the column should be displayed or not. Buttons and keyboard shortcuts are
 * also added to let the user select none, all or a default set of columns. The default set of
 * columns is defined in the table model. The model also defines the columns that cannot be
 * disabled.
 * </p>
 */
public final class TableColumnChooser extends GUIDialog {
    private static final long serialVersionUID = -9173600303234433460L;

    private final ChoosableColumnTableModel tableModel;

    public TableColumnChooser(NMONVisualizerGui gui, JFrame parent, ChoosableColumnTableModel choosableTableModel) {
        super(gui, parent, "Select Columns");

        setModal(true);
        setIconImage(Styles.buildIcon("table.png").getImage());
        setLayout(new BorderLayout());

        this.tableModel = choosableTableModel;

        // do not allow column names (column 0) to be edited
        // only allow changing the checkbox if the column can be disabled; note that checkbox should
        // not show up anyway - see CheckBoxCellRenderer
        DefaultTableModel model = new DefaultTableModel() {
            private static final long serialVersionUID = 947165395586168365L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1 && tableModel.canDisableColumn(row);
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class;
                }
                else {
                    return String.class;
                }
            }
        };

        model.addColumn("Column");
        model.addColumn("Display?");

        for (String columnName : tableModel.getAllColumns()) {
            model.addRow(new Object[] { columnName, Boolean.valueOf(tableModel.getEnabled(columnName)) });
        }

        GUITable table = new GUITable(gui, model);

        table.getColumnModel().getColumn(0).setMinWidth(100);
        table.getColumnModel().getColumn(1).setMaxWidth(60);

        table.setDefaultRenderer(Boolean.class, new CheckBoxCellRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setPreferredSize(new java.awt.Dimension(200, 150));
        scrollPane.setBorder(Styles.DOUBLE_LINE_BORDER);

        add(scrollPane, BorderLayout.CENTER);

        JButton all = new JButton("All");
        all.addActionListener(allAction);

        JButton none = new JButton("None");
        none.setIcon(Styles.CLEAR_ICON);
        none.addActionListener(noneAction);

        JButton defaults = new JButton("Default");
        defaults.addActionListener(defaultsAction);

        JPanel temp = new JPanel();
        temp.add(all);
        temp.add(none);
        temp.add(defaults);

        add(temp, BorderLayout.PAGE_START);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultTableModel model = getModel();

                for (int i = 0; i < model.getRowCount(); i++) {
                    TableColumnChooser.this.tableModel.setEnabled((String) model.getValueAt(i, 0),
                            (Boolean) model.getValueAt(i, 1));
                }

                dispose();
            }
        });

        temp = new JPanel();
        temp.add(ok);
        add(temp, BorderLayout.PAGE_END);

        getRootPane().setDefaultButton(ok);

        JPanel content = (JPanel) getContentPane();

        ActionMap actions = content.getActionMap();
        InputMap inputs = content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        actions.put("all", allAction);
        actions.put("none", noneAction);
        actions.put("defaults", defaultsAction);

        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "all");
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "none");
        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "defaults");

        setVisible(true);
    }

    private final AbstractAction allAction = new AbstractAction() {
        private static final long serialVersionUID = 5118961708188081301L;

        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = getModel();

            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(true, i, 1);
            }
        }
    };

    private final AbstractAction noneAction = new AbstractAction() {
        private static final long serialVersionUID = -6547510475096869295L;

        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = getModel();

            for (int i = 0; i < model.getRowCount(); i++) {
                if (tableModel.canDisableColumn(i)) {
                    model.setValueAt(false, i, 1);
                }
            }
        }
    };

    private final AbstractAction defaultsAction = new AbstractAction() {
        private static final long serialVersionUID = -826694390009398839L;

        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultTableModel model = getModel();

            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(tableModel.getDefaultColumnState(i), i, 1);
            }
        }
    };

    private final DefaultTableModel getModel() {
        return (DefaultTableModel) ((GUITable) ((JScrollPane) TableColumnChooser.this.getContentPane().getComponent(0))
                .getViewport().getComponent(0)).getModel();
    }

    private final class CheckBoxCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            JComponent renderer = null;

            boolean red = false;

            // note 'row' here since each column in the summary table is a row in this chooser table
            if (!tableModel.canDisableColumn(row)) {
                JLabel required = new JLabel("Required");

                required.setHorizontalAlignment(SwingConstants.CENTER);

                if (!isSelected) {
                    red = true;
                }

                renderer = required;
            }
            else {
                Boolean b = (Boolean) value;

                JCheckBox checkBox = new JCheckBox();
                checkBox.setHorizontalAlignment(SwingConstants.CENTER);

                if (b != null) {
                    checkBox.setSelected(b);
                }

                renderer = checkBox;
            }

            // the default check box renderer does not use the correct, alternating colors
            // this code is modified from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6723524
            Component other = (JLabel) table.getDefaultRenderer(String.class).getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);
            java.awt.Color bg = other.getBackground();

            if (isSelected) {
                renderer.setForeground(table.getSelectionForeground());
                renderer.setBackground(table.getSelectionBackground());
            }
            else {
                renderer.setForeground(red ? java.awt.Color.RED : other.getForeground());
                renderer.setBackground(new java.awt.Color(bg.getRed(), bg.getGreen(), bg.getBlue()));
            }
            renderer.setOpaque(true);

            return renderer;
        }
    }
}
