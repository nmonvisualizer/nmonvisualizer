package com.ibm.nmon.gui.interval;

import java.awt.Component;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import javax.swing.SwingConstants;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataSetListener;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.util.TimeFormatCache;

/**
 * JPanel holder for a combo box of all the Intervals and a button to bring up an interval
 * management dialog. Selecting an interval sets the current value for the application.
 */
public final class IntervalPicker extends JPanel implements DataSetListener {
    private final NMONVisualizerGui gui;

    private final JComboBox intervals;

    public IntervalPicker(NMONVisualizerGui gui) {
        assert gui != null;

        this.gui = gui;

        JLabel label = new JLabel("Interval:");
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        label.setFont(Styles.LABEL);

        add(label);

        intervals = new JComboBox(new IntervalComboBoxModel(gui.getIntervalManager()));
        intervals.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                Interval i = (Interval) value;
                setText(TimeFormatCache.formatInterval(i));

                return c;
            }
        });

        add(intervals);

        JButton manageIntervals = new JButton("Manage");
        manageIntervals.setIcon(Styles.INTERVAL_ICON);

        manageIntervals.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new IntervalManagerDialog(IntervalPicker.this.gui).setVisible(true);
            }
        });

        add(manageIntervals);

        gui.addPropertyChangeListener("timeZone", (IntervalComboBoxModel) intervals.getModel());
        gui.addDataSetListener(this);
    }

    public void dataAdded(DataSet data) {
        updateOnDataChange();
    }

    public void dataRemoved(DataSet data) {
        updateOnDataChange();
    }

    public void dataChanged(DataSet data) {
        updateOnDataChange();
    }

    public void dataCleared() {
        updateOnDataChange();
    }

    private void updateOnDataChange() {
        ((IntervalComboBoxModel) intervals.getModel()).propertyChange(null);
    }
}
