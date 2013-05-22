package com.ibm.nmon.gui.interval;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeListener;

import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import javax.swing.SwingConstants;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.data.DataSetListener;
import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.interval.IntervalManager;
import com.ibm.nmon.interval.Interval;

/**
 * <p>
 * Base implementation for a panel that supports entering times to create a new {@link Interval}.
 * <ul>
 * <li>Creates common labels, text fields and buttons for use by subclasses.</li>
 * <li>Defines a common API for setting start and end times for the interval.</li>
 * <li>Listens for {@link DataSetListener DataSet events} and updates the start and end times based
 * on the new application min and max times.</li>
 * </ul>
 * <p>
 * 
 * <p>
 * Subclasses must implement
 * {@link PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent) propertyChange()}
 * for <code>timeZone</code> events. When the time zone changes, the displayed start and end times
 * should change to reflect the new time zone.
 * </p>
 */
abstract class BaseIntervalPanel extends JPanel implements DataSetListener, PropertyChangeListener {
    protected final NMONVisualizerGui gui;

    protected final JTextField name;
    protected final JLabel nameLabel;

    protected final JButton add;
    protected final JButton reset;

    protected final JButton endToStart;

    BaseIntervalPanel(NMONVisualizerGui gui) {
        super();

        this.gui = gui;

        name = new JTextField(10);
        nameLabel = new JLabel("Interval Name:");
        nameLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        nameLabel.setFont(Styles.LABEL);

        add = new JButton("Add");
        add.setIcon(Styles.ADD_ICON);

        reset = new JButton("Reset");
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setTimes(getDefaultStartTime(), getDefaultEndTime());
            }
        });

        AbstractAction endToStartAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setStartToEnd();
            }
        };

        endToStart = new JButton("Start = End");
        endToStart.setIcon(Styles.buildIcon("arrow_up.png"));
        endToStart.addActionListener(endToStartAction);

        ActionMap actions = getActionMap();
        InputMap inputs = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        actions.put("endToStart", endToStartAction);

        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "endToStart");
    }

    final String getIntervalName() {
        return name.getText();
    }

    final void setIntervalName(String name) {
        this.name.setText(name);
    }

    /**
     * @return a default value for the start time, in milliseconds
     * 
     * @see {@link NMONVisualizerApp#getMinSystemTime()}
     */
    protected final long getDefaultStartTime() {
        if (gui.getDataSetCount() > 0) {
            return gui.getMinSystemTime();
        }
        else {
            // rounding off milliseconds
            return ((System.currentTimeMillis() / 1000) - (86400 / 2)) * 1000;
        }
    }

    /**
     * @return a default value for the end time, in milliseconds
     * 
     * @see {@link NMONVisualizerApp#getMaxSystemTime()}
     */
    protected final long getDefaultEndTime() {
        if (gui.getDataSetCount() > 0) {
            return gui.getMaxSystemTime();
        }
        else {
            // rounding off milliseconds
            return ((System.currentTimeMillis() / 1000) + (86400 / 2)) * 1000;
        }
    }

    /**
     * @return the currently set start time, in milliseconds
     */
    abstract long getStartTime();

    /**
     * @return the currently set end time, in milliseconds
     */
    abstract long getEndTime();

    /**
     * Set the start and end times using milliseconds
     */
    abstract void setTimes(long start, long end);

    /**
     * Get the current time zone used for formatting displayed time values.
     */
    abstract TimeZone getTimeZone();

    /**
     * Set the current end time to the current end time. This will not be a valid interval since the
     * start time will now equal the end time.
     */
    protected abstract void setStartToEnd();

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);

            if (enabled) {
                gui.addDataSetListener(this);
                gui.addPropertyChangeListener("timeZone", this);
                getRootPane().setDefaultButton(add);
            }
            else {
                gui.removeDataSetListener(this);
                gui.removePropertyChangeListener("timeZone", this);
            }
        }
    }

    public final void dataAdded(DataSet file) {
        setTimes(getDefaultStartTime(), getDefaultEndTime());
    }

    public final void dataRemoved(DataSet data) {
        setTimes(getDefaultStartTime(), getDefaultEndTime());
    }

    public final void dataChanged(DataSet data) {
        setTimes(getDefaultStartTime(), getDefaultEndTime());
    }

    public final void dataCleared() {
        setTimes(getDefaultStartTime(), getDefaultEndTime());
    }

    protected final void requestFocus(final JFormattedTextField field) {
        field.requestFocus();

        // hack to fix formatted text fields receiving focus programatically but not allowing
        // formatted edits
        try {
            field.commitEdit();
        }
        catch (java.text.ParseException pe) {
            // ignore - value should be valid since formatter checked when focus was previously lost
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                field.selectAll();
            }
        });
    }

    /**
     * <p>
     * Used by subclasses to allow multiple components to add a new interval.
     * </p>
     * 
     * <p>
     * Fires an <code>interval</code> property change event and calls
     * {@link IntervalManager#setCurrentInterval(Interval) setCurrentInterval()} when an interval is
     * added.
     * </p>
     */
    protected final ActionListener addInterval = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            long startTime = getStartTime();
            long endTime = getEndTime();

            // only add if not the default interval
            if ((startTime != gui.getMinSystemTime()) || (endTime != gui.getMaxSystemTime())) {
                IntervalManager intervalManager = gui.getIntervalManager();
                Interval i = null;

                try {
                    i = new Interval(startTime, endTime);
                    i.setName(name.getText());
                }
                catch (IllegalArgumentException iae) {
                    JOptionPane.showMessageDialog(BaseIntervalPanel.this.getParent(), "Start time"
                            + " must be less than " + " End time", "Invalid Time", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (intervalManager.addInterval(i)) {
                    firePropertyChange("interval", intervalManager.getCurrentInterval(), i);

                    intervalManager.setCurrentInterval(i);
                }
            }
        }
    };
}
