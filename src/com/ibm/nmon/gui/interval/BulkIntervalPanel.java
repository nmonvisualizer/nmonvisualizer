package com.ibm.nmon.gui.interval;

import java.awt.BorderLayout;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.JSpinner.DateEditor;

import javax.swing.SwingConstants;

import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.gui.time.TimeMaskFormatter;

import com.ibm.nmon.interval.IntervalManager;
import com.ibm.nmon.interval.Interval;

import com.ibm.nmon.gui.Styles;

/**
 * Panel for entering multiple intervals using an absolute start time and a repeating duration.
 * Offsets between each interval are also supported.
 */
public final class BulkIntervalPanel extends BaseIntervalPanel {
    private static final long serialVersionUID = 1817418187436308391L;

    // base date time
    private final JSpinner start;

    private final JFormattedTextField duration;
    private final JFormattedTextField days;
    private final JFormattedTextField repeat;
    private final JFormattedTextField offset;
    private final JLabel end;

    private final JLabel durationLabel;
    private final JLabel repeatLabel;
    private final JLabel offsetLabel;

    private final SimpleDateFormat FORMAT = new SimpleDateFormat(Styles.DATE_FORMAT_STRING);

    private final BulkDocumentListener durationListener;
    private final BulkDocumentListener daysListener;
    private final BulkDocumentListener repeatListener;
    private final BulkDocumentListener offsetListener;

    public BulkIntervalPanel(NMONVisualizerGui gui) {
        super(gui);

        setLayout(new BorderLayout());

        JLabel startLabel = new JLabel("Start:");
        startLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        startLabel.setFont(Styles.LABEL);

        start = new JSpinner(new SpinnerDateModel(new Date(getDefaultStartTime()), null, null, Calendar.MINUTE));
        start.setEditor(new DateEditor(start, Styles.DATE_FORMAT_STRING_WITH_YEAR));

        JLabel endLabel = new JLabel("End:");
        endLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        endLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        endLabel.setFont(Styles.LABEL);

        end = new JLabel();
        end.setFont(Styles.BOLD);
        // end.setVerticalAlignment(SwingConstants.TOP);

        DefaultFormatter formatter = new DefaultFormatter();
        formatter.setValueClass(Integer.class);

        durationLabel = new JLabel("Duration:");
        durationLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        durationLabel.setFont(Styles.LABEL);

        duration = new JFormattedTextField();
        duration.setFormatterFactory(TimeMaskFormatter.createFormatterFactory(true));
        duration.setColumns(5);
        duration.setValue(0);

        days = new JFormattedTextField();
        days.setFormatterFactory(new DefaultFormatterFactory(formatter));
        // assume a small number of days will be entered
        days.setColumns(3);
        days.setValue(1);
        days.setHorizontalAlignment(SwingConstants.TRAILING);

        repeatLabel = new JLabel("Repeat:");
        repeatLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        repeatLabel.setFont(Styles.LABEL);

        repeat = new JFormattedTextField();
        repeat.setFormatterFactory(new DefaultFormatterFactory(formatter));
        // assume a small number of repeats will be entered
        repeat.setColumns(3);
        repeat.setHorizontalAlignment(SwingConstants.TRAILING);
        repeat.setValue(2);

        offsetLabel = new JLabel("Time Between:");
        offsetLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        offsetLabel.setFont(Styles.LABEL);

        offset = new JFormattedTextField();
        offset.setFormatterFactory(TimeMaskFormatter.createFormatterFactory(true));
        offset.setColumns(5);
        offset.setValue(0);

        // set last after other fields that determine the end time are setup
        end.setText((FORMAT.format(new Date(getEndTime()))));

        JButton hourly = new JButton("1 Hour");
        hourly.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Calendar cal = Calendar.getInstance(BulkIntervalPanel.this.gui.getDisplayTimeZone());
                cal.setTime((Date) start.getValue());
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                start.setValue(cal.getTime());

                duration.setValue(3600);
                days.setValue(0);

                requestFocus(repeat);
            }
        });

        JButton daily = new JButton("1 Day");
        daily.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Calendar cal = Calendar.getInstance(BulkIntervalPanel.this.gui.getDisplayTimeZone());
                cal.setTime((Date) start.getValue());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                start.setValue(cal.getTime());

                duration.setValue(0);
                days.setValue(1);

                requestFocus(repeat);
            }
        });

        // main panel contains name at top, buttons at bottom, all others in the CENTER
        JPanel namePanel = new JPanel();
        namePanel.add(nameLabel);
        namePanel.add(name);

        JPanel centerPanel = new JPanel(new GridBagLayout());

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

        // stretch out start and end times
        fieldConstraints.gridwidth = 2;

        centerPanel.add(startLabel, labelConstraints);
        centerPanel.add(start, fieldConstraints);
        labelConstraints.gridx += 3;
        fieldConstraints.gridx += 3;
        centerPanel.add(endLabel, labelConstraints);
        centerPanel.add(end, fieldConstraints);

        labelConstraints.gridx = 0;
        fieldConstraints.gridx = 1;

        fieldConstraints.gridwidth = 1;
        fieldConstraints.gridx = 1;

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;

        centerPanel.add(durationLabel, labelConstraints);
        centerPanel.add(duration, fieldConstraints);
        ++fieldConstraints.gridx;

        fieldConstraints.gridx = 1;

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;

        centerPanel.add(durationLabel, labelConstraints);
        centerPanel.add(duration, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(new JLabel("HH:mm:ss"), fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(days, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(new JLabel("days"), fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(hourly, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(daily, fieldConstraints);

        fieldConstraints.gridx = 1;

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;

        centerPanel.add(repeatLabel, labelConstraints);
        centerPanel.add(repeat, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(new JLabel("times"), fieldConstraints);

        fieldConstraints.gridx = 1;

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;

        centerPanel.add(offsetLabel, labelConstraints);
        centerPanel.add(offset, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(new JLabel("HH:mm:ss"), fieldConstraints);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(add);
        buttonsPanel.add(endToStart);
        buttonsPanel.add(reset);

        add(namePanel, BorderLayout.PAGE_START);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.PAGE_END);

        // override the BaseIntervalPanel action to instead create multiple intervals
        ActionListener addIntervals = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int repeatCount = (Integer) repeat.getValue();

                if (repeatCount == 0) {
                    return;
                }

                // arbitrary limit to keep users from entering huge numbers
                if (repeatCount > 99) {
                    JOptionPane.showMessageDialog(BulkIntervalPanel.this.getParent(),
                            "Repeat count must be less than 100", "Large Repeat Count", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                long durationMillis = ((Integer) duration.getValue() * 1000L) + ((Integer) days.getValue() * 86400000L);

                if (durationMillis == 0) {
                    return;
                }

                long startTime = getStartTime();

                IntervalManager intervalManager = BulkIntervalPanel.this.gui.getIntervalManager();
                Interval interval = null;

                for (int i = 0; i < repeatCount; i++) {
                    long endTime = startTime + durationMillis;

                    interval = new Interval(startTime, endTime);

                    // append a number to each name
                    if (!"".equals(name.getText())) {
                        interval.setName(name.getText() + ' ' + (i + 1));
                    }

                    startTime = endTime + ((Integer) offset.getValue() * 1000L);

                    if (intervalManager.addInterval(interval)) {
                        firePropertyChange("interval", intervalManager.getCurrentInterval(), interval);
                    }
                }

                intervalManager.setCurrentInterval(interval);
            }
        };

        add.addActionListener(addIntervals);

        durationListener = new BulkDocumentListener(this, duration, duration, days, repeat, offset);
        daysListener = new BulkDocumentListener(this, days, duration, days, repeat, offset);
        repeatListener = new BulkDocumentListener(this, repeat, duration, days, repeat, offset);
        offsetListener = new BulkDocumentListener(this, repeat, duration, days, repeat, offset);

        duration.getDocument().addDocumentListener(durationListener);
        days.getDocument().addDocumentListener(daysListener);
        repeat.getDocument().addDocumentListener(repeatListener);
        offset.getDocument().addDocumentListener(offsetListener);

        durationListener.addPropertyChangeListener(this);
        daysListener.addPropertyChangeListener(this);
        repeatListener.addPropertyChangeListener(this);
        offsetListener.addPropertyChangeListener(this);

        start.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                long startTime = getStartTime();
                long endTime = getEndTime();

                if (startTime < endTime) {
                    Interval i = new Interval(startTime, endTime);

                    firePropertyChange("interval", null, i);
                    end.setText(FORMAT.format(new Date(endTime)));
                }
            }
        });
    }

    @Override
    protected long getEndTime() {
        return getEndTime(getStartTime(), (Integer) duration.getValue(), (Integer) days.getValue(),
                (Integer) repeat.getValue(), (Integer) offset.getValue());
    }

    long getEndTime(long startTime, int duration, int days, int repeat, int offset) {
        // subtract one offset because it is not needed on last interval
        return startTime + ((repeat * (duration + days * 86400 + offset)) - offset) * 1000;
    }

    @Override
    long getStartTime() {
        return ((Date) start.getValue()).getTime();
    }

    @Override
    TimeZone getTimeZone() {
        DateEditor de = (DateEditor) start.getEditor();
        return de.getFormat().getTimeZone();
    }

    @Override
    protected void setStartToEnd() {
        start.setValue(new Date(getEndTime()));
        requestFocus(duration);
    }

    @Override
    protected void setTimes(long start, long end) {
        if (end > start) {
            int offsetMillis = (Integer) offset.getValue();
            int repeatCount = (Integer) repeat.getValue();
            long durationMillis = 0;

            if (repeatCount == 0) {
                durationMillis = end - start - offsetMillis;
            }
            else {
                durationMillis = (end - start - (offsetMillis * (repeatCount - 1))) / repeatCount;
            }

            int numDays = (int) (durationMillis / 86400000L);
            int endTime = (int) (durationMillis / 1000 % 86400);

            days.setValue(numDays);
            duration.setValue(endTime);
        }
        else {
            duration.setValue(0);
            days.setValue(0);

            // update end here since it will not update in propertyChange() with an invalid itnerval
            this.end.setText((FORMAT.format(new Date(end))));
        }

        // set start last since it fires a ChangeEvent which needs the new end time
        this.start.setValue(new Date(start));

        requestFocus(duration);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            requestFocus(duration);
        }
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // document listeners should also propagate property changes to listeners
        durationListener.addPropertyChangeListener(listener);
        daysListener.addPropertyChangeListener(listener);
        repeatListener.addPropertyChangeListener(listener);
        offsetListener.addPropertyChangeListener(listener);

        super.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("timeZone".equals(evt.getPropertyName())) {
            TimeZone timeZone = (TimeZone) evt.getNewValue();

            DateEditor de = (DateEditor) start.getEditor();
            de.getFormat().setTimeZone(timeZone);

            // hack to get the spinner to fire a state change and update the displayed value
            // toggle the calendar field back to its original value
            ((SpinnerDateModel) start.getModel()).setCalendarField(Calendar.MINUTE);
            ((SpinnerDateModel) start.getModel()).setCalendarField(Calendar.SECOND);

            FORMAT.setTimeZone(timeZone);
            end.setText(FORMAT.format(getEndTime()));
        }
        else if ("values".equals(evt.getPropertyName())) {
            long[] updatedValues = (long[]) evt.getNewValue();

            boolean validDuration = true;

            // 0 days, then duration must be valid & non-zero
            if (updatedValues[1] == 0) {
                // duration
                if (updatedValues[0] < 1) {
                    validDuration = false;
                }
            }

            if (validDuration) {
                durationLabel.setFont(Styles.LABEL);
                durationLabel.setForeground(Styles.DEFAULT_COLOR);

                duration.setForeground(Styles.DEFAULT_COLOR);
                days.setForeground(Styles.DEFAULT_COLOR);
            }
            else {
                durationLabel.setFont(Styles.LABEL_ERROR);
                durationLabel.setForeground(Styles.ERROR_COLOR);

                duration.setForeground(Styles.ERROR_COLOR);
                days.setForeground(Styles.ERROR_COLOR);
            }

            // repeat
            if (updatedValues[2] == 0) {
                repeatLabel.setFont(Styles.LABEL_ERROR);
                repeatLabel.setForeground(Styles.ERROR_COLOR);

                repeat.setForeground(Styles.ERROR_COLOR);

                offset.setEnabled(false);
            }
            else {
                repeatLabel.setFont(Styles.LABEL);
                repeatLabel.setForeground(Styles.DEFAULT_COLOR);

                repeat.setForeground(Styles.DEFAULT_COLOR);

                if (updatedValues[2] == 1) {
                    // repeating once => no offset
                    offset.setEnabled(false);
                }
                else {
                    offset.setEnabled(true);
                }
            }

            // offset: -1 => invalid
            if (updatedValues[3] < 0) {
                offsetLabel.setFont(Styles.LABEL_ERROR);
                offsetLabel.setForeground(Styles.ERROR_COLOR);

                offset.setForeground(Styles.ERROR_COLOR);
            }
            else {
                offsetLabel.setFont(Styles.LABEL);
                offsetLabel.setForeground(Styles.DEFAULT_COLOR);

                offset.setForeground(Styles.DEFAULT_COLOR);
            }

            // end time
            if (updatedValues[4] != -1) {
                end.setText(FORMAT.format(updatedValues[4]));
            }
        }
    }
}
