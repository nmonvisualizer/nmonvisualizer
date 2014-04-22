package com.ibm.nmon.gui.interval;

import java.beans.PropertyChangeEvent;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.JSpinner.DateEditor;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.interval.Interval;

/**
 * Panel for entering intervals using absolute date times
 */
final class AbsoluteTimeIntervalPanel extends BaseIntervalPanel {
    private static final long serialVersionUID = 3451148920350034946L;

    private final JSpinner start;
    private final JSpinner end;

    private final JLabel startLabel;
    private final JLabel endLabel;

    AbsoluteTimeIntervalPanel(NMONVisualizerGui gui) {
        super(gui);

        setLayout(new BorderLayout());

        add.addActionListener(addInterval);

        // start and end text boxes with labels, followed by a Add button
        startLabel = new JLabel("Start:");
        startLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        startLabel.setFont(Styles.LABEL);

        start = new JSpinner(new SpinnerDateModel(new Date(getDefaultStartTime()), null, null, Calendar.MINUTE));
        start.setEditor(new DateEditor(start, Styles.DATE_FORMAT_STRING_WITH_YEAR));
        start.addChangeListener(intervalUpdater);

        endLabel = new JLabel("End:");
        endLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        endLabel.setFont(Styles.LABEL);

        end = new JSpinner(new SpinnerDateModel(new Date(getDefaultEndTime()), null, null, Calendar.MINUTE));
        end.setEditor(new DateEditor(end, Styles.DATE_FORMAT_STRING_WITH_YEAR));
        end.addChangeListener(intervalUpdater);

        JPanel namePanel = new JPanel();

        namePanel.add(nameLabel);
        namePanel.add(name);

        JPanel startPanel = new JPanel();

        startPanel.add(startLabel);
        startPanel.add(start);

        JPanel endPanel = new JPanel();

        endPanel.add(endLabel);
        endPanel.add(end);

        JPanel buttonsPanel = new JPanel();

        buttonsPanel.add(add);
        buttonsPanel.add(endToStart);
        buttonsPanel.add(reset);

        JPanel dataPanel = new JPanel();
        dataPanel.setLayout(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        GridBagConstraints fieldConstraints = new GridBagConstraints();

        labelConstraints.gridx = 0;
        fieldConstraints.gridx = 1;

        labelConstraints.gridy = 0;
        fieldConstraints.gridy = 0;

        labelConstraints.insets = new Insets(0, 0, 0, 5);
        fieldConstraints.insets = new Insets(5, 0, 0, 5);

        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;

        dataPanel.add(startLabel, labelConstraints);
        dataPanel.add(start, fieldConstraints);

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;

        dataPanel.add(endLabel, labelConstraints);
        dataPanel.add(end, fieldConstraints);

        add(namePanel, BorderLayout.PAGE_START);
        add(dataPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.PAGE_END);
    }

    @Override
    long getStartTime() {
        return ((Date) start.getValue()).getTime();
    }

    @Override
    long getEndTime() {
        return ((Date) end.getValue()).getTime();
    }

    void setTimes(long start, long end) {
        this.start.setValue(new java.util.Date(start));
        this.end.setValue(new java.util.Date(end));
    }

    @Override
    TimeZone getTimeZone() {
        DateEditor de = (DateEditor) start.getEditor();
        return de.getFormat().getTimeZone();
    }

    @Override
    protected void setStartToEnd() {
        start.setValue(end.getValue());

        ((JSpinner.DefaultEditor) end.getEditor()).getTextField().requestFocus();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            ((JSpinner.DefaultEditor) start.getEditor()).getTextField().requestFocus();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("timeZone".equals(evt.getPropertyName())) {
            TimeZone timeZone = (TimeZone) evt.getNewValue();

            DateEditor de = (DateEditor) start.getEditor();
            de.getFormat().setTimeZone(timeZone);

            // hack to get the spinner to fire a state change and update the displayed value
            ((SpinnerDateModel) start.getModel()).setCalendarField(Calendar.MINUTE);
            ((SpinnerDateModel) start.getModel()).setCalendarField(Calendar.SECOND);

            de = (DateEditor) end.getEditor();
            de.getFormat().setTimeZone(timeZone);

            ((SpinnerDateModel) end.getModel()).setCalendarField(Calendar.MINUTE);
            ((SpinnerDateModel) end.getModel()).setCalendarField(Calendar.SECOND);
        }
    }

    // update the interval when the start or end time changes
    private final ChangeListener intervalUpdater = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            long startTime = getStartTime();
            long endTime = getEndTime();

            if (endTime > startTime) {
                startLabel.setFont(Styles.LABEL);
                endLabel.setFont(Styles.LABEL);

                startLabel.setForeground(Styles.DEFAULT_COLOR);
                endLabel.setForeground(Styles.DEFAULT_COLOR);

                ((JSpinner.DefaultEditor) start.getEditor()).getTextField().setForeground(Styles.DEFAULT_COLOR);
                ((JSpinner.DefaultEditor) end.getEditor()).getTextField().setForeground(Styles.DEFAULT_COLOR);

                Interval i = new Interval(startTime, endTime);
                firePropertyChange("interval", null, i);
            }
            else {
                startLabel.setFont(Styles.LABEL_ERROR);
                endLabel.setFont(Styles.LABEL_ERROR);

                startLabel.setForeground(Styles.ERROR_COLOR);
                endLabel.setForeground(Styles.ERROR_COLOR);

                ((JSpinner.DefaultEditor) start.getEditor()).getTextField().setForeground(Styles.ERROR_COLOR);
                ((JSpinner.DefaultEditor) end.getEditor()).getTextField().setForeground(Styles.ERROR_COLOR);

                firePropertyChange("interval", null, null);
            }
        }
    };
}
