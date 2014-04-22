package com.ibm.nmon.gui.interval;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.HierarchyEvent;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.KeyStroke;

import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;

import com.ibm.nmon.gui.GUITable;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.table.StringCellRenderer;

import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.util.TimeFormatCache;

/**
 * <p>
 * Display a list of Intervals in a 3 column table. Also contains a button to clear all the current
 * intervals.
 * </p>
 * 
 * <p>
 * Users can update the interval name directly in the table. Selecting an interval in the table will
 * fire an <code>interval</code> property change event.
 * </p>
 * 
 * <p>
 * The table will update on time zone changes.
 * </p>
 */
final class IntervalList extends JPanel implements HierarchyListener, PropertyChangeListener {
    private static final long serialVersionUID = -2227815465031324337L;

    private final GUITable intervals;

    private final NMONVisualizerGui gui;

    public IntervalList(NMONVisualizerGui gui) {
        super();

        setLayout(new BorderLayout());

        this.gui = gui;

        IntervalTableModel model = new IntervalTableModel(gui);

        intervals = new GUITable(gui, model);
        // update the interval name
        intervals.getCellEditor(0, 0).addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                int selectedRow = intervals.getSelectedRow();

                Interval i = ((IntervalTableModel) intervals.getModel()).getValueAt(selectedRow);
                String newName = (String) intervals.getCellEditor(selectedRow, 0).getCellEditorValue();

                IntervalList.this.gui.getIntervalManager().renameInterval(i, newName);
            }

            @Override
            public void editingCanceled(ChangeEvent e) {}
        });

        // make the current interval bold
        intervals.setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 7101632198507145234L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                setText(TimeFormatCache.formatDateTime((Long) value));

                Interval i = ((IntervalTableModel) table.getModel()).getValueAt(row);

                if (i.equals(IntervalList.this.gui.getIntervalManager().getCurrentInterval())) {
                    setFont(getFont().deriveFont(Font.BOLD));
                }

                return this;
            }
        });
        intervals.setDefaultRenderer(String.class, new StringCellRenderer());

        intervals.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // fire an interval change when the table selection changes
        intervals.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int row = intervals.getSelectedRow();
                    Interval interval = null;

                    if (row >= 0) {
                        interval = ((IntervalTableModel) intervals.getModel()).getValueAt(row);
                    }
                    else {
                        // nothing selected => no rows in table => no intervals => default interval
                        // note that current interval has not been updated yet, so manually set to
                        // default
                        interval = Interval.DEFAULT;
                    }

                    IntervalList.this.firePropertyChange("interval", null, interval);
                }
            }
        });

        JLabel label = new JLabel("Intervals");
        label.setFont(Styles.LABEL);
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 0));

        add(label, BorderLayout.PAGE_START);

        JScrollPane scrollPane = new JScrollPane(intervals);
        scrollPane.getViewport().setBackground(java.awt.Color.WHITE);
        scrollPane.setPreferredSize(new java.awt.Dimension(600, 150));
        scrollPane.setBorder(Styles.DOUBLE_LINE_BORDER);

        add(scrollPane, BorderLayout.CENTER);

        RemoveAllIntervalsAction removeAllAction = new RemoveAllIntervalsAction(gui, this);

        final JButton clear = new JButton("Clear");
        clear.setIcon(Styles.CLEAR_ICON);
        clear.addActionListener(removeAllAction);
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IntervalList.this.firePropertyChange("interval", null, IntervalList.this.gui.getIntervalManager()
                        .getCurrentInterval());
            }
        });

        // temp panel to keep the button from filling the space
        JPanel temp = new JPanel();
        temp.add(clear);

        add(temp, BorderLayout.PAGE_END);

        getActionMap().put("clear", new AbstractAction() {
            private static final long serialVersionUID = 317117736111534324L;

            @Override
            public void actionPerformed(ActionEvent e) {
                clear.doClick();
            }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK), "clear");
    }

    GUITable getTable() {
        return intervals;
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            if (e.getChanged().isVisible()) {
                gui.addPropertyChangeListener("timeZone", this);
                gui.getIntervalManager().addListener((IntervalTableModel) intervals.getModel());
            }
            else {
                gui.removePropertyChangeListener("timeZone", this);
                gui.getIntervalManager().removeListener((IntervalTableModel) intervals.getModel());
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("timeZone".equals(evt.getPropertyName())) {
            // force a redraw of the table so it will redisplay the updated times
            ((IntervalTableModel) intervals.getModel()).fireTableDataChanged();
        }
    }
}
