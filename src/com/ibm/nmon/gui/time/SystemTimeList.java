package com.ibm.nmon.gui.time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.awt.BorderLayout;
import java.awt.Color;

import java.awt.event.HierarchyListener;
import java.awt.event.HierarchyEvent;

import javax.swing.BorderFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import javax.swing.ListSelectionModel;

import com.ibm.nmon.gui.GUITable;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.interval.Interval;

/**
 * <p>
 * JPanel containing a list of the current {@link DataSet DataSets} and their start and end times.
 * The number of records the data set contains for an interval is also shown.
 * <p>
 * 
 * <p>
 * The given interval is set by adding this class as a {@link PropertyChangeListener} for
 * <code>interval</code> events. This interval can be null to indicate an invalid value. The UI will
 * change to indicate this by graying out the table.
 * </p>
 */
public final class SystemTimeList extends JPanel implements HierarchyListener, PropertyChangeListener {
    private static final long serialVersionUID = -7601601214911823159L;

    private final NMONVisualizerGui gui;

    private final GUITable systemTimes;
    private final SystemTimeTableCellRenderer renderer;

    private final Color defaultTableColor;

    public SystemTimeList(final NMONVisualizerGui gui) {
        super(new BorderLayout());

        this.gui = gui;

        renderer = new SystemTimeTableCellRenderer();
        renderer.setIntervalToCompare(gui.getIntervalManager().getCurrentInterval());

        SystemTimeTableModel model = new SystemTimeTableModel(gui);

        systemTimes = new GUITable(gui, model);
        systemTimes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        systemTimes.getColumnModel().getColumn(0).setPreferredWidth(200);
        systemTimes.getColumnModel().getColumn(1).setPreferredWidth(100);
        systemTimes.getColumnModel().getColumn(2).setPreferredWidth(100);
        systemTimes.getColumnModel().getColumn(3).setPreferredWidth(75);
        systemTimes.setDefaultRenderer(String.class, renderer);
        systemTimes.setDefaultRenderer(Long.class, renderer);
        systemTimes.setDefaultRenderer(Integer.class, renderer);

        JLabel label = new JLabel("System Times");
        label.setFont(Styles.LABEL);
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 0));

        add(label, BorderLayout.PAGE_START);

        JScrollPane scrollPane = new JScrollPane(systemTimes);
        scrollPane.getViewport().setBackground(java.awt.Color.WHITE);
        scrollPane.setPreferredSize(new java.awt.Dimension(600, 250));
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.LIGHT_GRAY));

        add(scrollPane, BorderLayout.CENTER);

        // save the default color for property change events
        defaultTableColor = systemTimes.getBackground();
    }

    // when the containing window is shown, start listening for events
    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            if (e.getChanged().isVisible()) {
                gui.addPropertyChangeListener("timeZone", this);
                gui.addDataSetListener((SystemTimeTableModel) systemTimes.getModel());
            }
            else {
                gui.removePropertyChangeListener("timeZone", this);
                gui.removeDataSetListener((SystemTimeTableModel) systemTimes.getModel());
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("interval".equals(evt.getPropertyName())) {
            SystemTimeTableModel model = (SystemTimeTableModel) systemTimes.getModel();

            Interval i = (Interval) evt.getNewValue();

            if (i != null) {
                if (!systemTimes.isEnabled()) {
                    // if the table color is not set to the default, the renderer will not render
                    // alternate table rows in a different color
                    systemTimes.setBackground(defaultTableColor);
                    systemTimes.setEnabled(true);

                    // chaging the model clears the row selection, so reapply it when done
                    int row = systemTimes.getSelectedRow();

                    if (row != -1) {
                        systemTimes.setRowSelectionInterval(row, row);
                    }
                }

                if (!model.getCountInterval().equals(i)) {

                    renderer.setIntervalToCompare(i);
                    model.setCountInterval(i);
                }
            }
            else {
                if (systemTimes.isEnabled()) {
                    // invalid interval, disable table + visual queue to user
                    systemTimes.setEnabled(false);
                    systemTimes.setBackground(java.awt.Color.LIGHT_GRAY);
                }
            }
        }
    }

    public GUITable getTable() {
        return systemTimes;
    }
}
