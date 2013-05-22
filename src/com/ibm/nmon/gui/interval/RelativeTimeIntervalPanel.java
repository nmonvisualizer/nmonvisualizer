package com.ibm.nmon.gui.interval;

import java.awt.BorderLayout;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import java.awt.Insets;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.JSpinner.DateEditor;

import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.time.TimeMaskFormatter;
import com.ibm.nmon.interval.Interval;

/**
 * Panel for entering intervals using relative times. Start and end times are specified in 24 hour
 * format. The start time is relative to a base datetime. The end time also supports specifying a
 * number of days. Also displays absolute time labels for the start and the end that update as the
 * user changes inputs.
 */
final class RelativeTimeIntervalPanel extends BaseIntervalPanel {
    // base date time
    private final JSpinner base;

    // start and end times in 24H format
    private final JFormattedTextField start;
    private final JFormattedTextField end;

    // days the interval covers; used with the end time
    private final JFormattedTextField days;

    private final JLabel startLabel;
    private final JLabel endLabel;

    private final JLabel startAbsolute;
    private final JLabel endAbsolute;

    private final SimpleDateFormat FORMAT = new SimpleDateFormat(Styles.DATE_FORMAT_STRING);

    private final RelativeTimeDocumentListener startListener;
    private final RelativeTimeDocumentListener endListener;
    private final RelativeTimeDocumentListener daysListener;

    RelativeTimeIntervalPanel(NMONVisualizerGui gui) {
        super(gui);

        setLayout(new BorderLayout());

        add.addActionListener(addInterval);

        JLabel baseLabel = new JLabel("Base Time:");
        baseLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        baseLabel.setFont(Styles.LABEL);

        base = new JSpinner(new SpinnerDateModel(new Date(getDefaultStartTime()), null, null, Calendar.MINUTE));
        base.setEditor(new DateEditor(base, Styles.DATE_FORMAT_STRING_WITH_YEAR));
        base.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                long startTime = getStartTime();
                long endTime = getEndTime();

                startAbsolute.setText(FORMAT.format(new Date(getStartTime())));
                endAbsolute.setText((FORMAT.format(new Date(getEndTime()))));

                if (startTime < endTime) {
                    Interval i = new Interval(startTime, endTime);

                    firePropertyChange("interval", null, i);
                }
            }
        });

        startLabel = new JLabel("Start:");
        startLabel.setFont(Styles.LABEL);
        startLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        start = new JFormattedTextField();
        start.setName("start");
        start.setFormatterFactory(TimeMaskFormatter.createFormatterFactory(true));
        // may not be enough columns with larger fonts
        start.setColumns(5);
        start.setValue(0);

        endLabel = new JLabel("End:");
        endLabel.setFont(Styles.LABEL);
        endLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        end = new JFormattedTextField();
        end.setName("end");
        end.setFormatterFactory(TimeMaskFormatter.createFormatterFactory(false));
        // may not be enough columns with larger fonts
        end.setColumns(5);
        end.setValue(0);

        DefaultFormatter formatter = new DefaultFormatter();
        formatter.setValueClass(Integer.class);

        days = new JFormattedTextField();
        days.setFormatterFactory(new DefaultFormatterFactory(formatter));
        // assume a small number of days will be entered
        days.setColumns(3);
        days.setValue(0);
        days.setHorizontalAlignment(SwingConstants.TRAILING);

        startAbsolute = new JLabel();
        startAbsolute.setFont(Styles.BOLD);

        endAbsolute = new JLabel();
        endAbsolute.setFont(Styles.BOLD);

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

        JLabel hhmmss = new JLabel("HH:mm:ss");

        // stretch out base time
        fieldConstraints.gridwidth = 2;

        centerPanel.add(baseLabel, labelConstraints);
        centerPanel.add(base, fieldConstraints);

        fieldConstraints.gridwidth = 1;
        fieldConstraints.gridx = 1;

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;

        // end also has days text box and label, so space out the absolute start label
        centerPanel.add(startLabel, labelConstraints);
        centerPanel.add(start, fieldConstraints);
        ++fieldConstraints.gridx;
        fieldConstraints.gridwidth = 3;
        centerPanel.add(hhmmss, fieldConstraints);
        // put absolute time in last column
        fieldConstraints.gridx += 3;
        fieldConstraints.gridwidth = 1;
        centerPanel.add(startAbsolute, fieldConstraints);

        fieldConstraints.gridx = 1;

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;

        hhmmss = new JLabel("HH:mm:ss");

        JLabel daysLabel = new JLabel("days");

        centerPanel.add(endLabel, labelConstraints);
        centerPanel.add(end, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(hhmmss, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(days, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(daysLabel, fieldConstraints);
        ++fieldConstraints.gridx;
        centerPanel.add(endAbsolute, fieldConstraints);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(add);
        buttonsPanel.add(endToStart);
        buttonsPanel.add(reset);

        add(namePanel, BorderLayout.PAGE_START);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.PAGE_END);

        // note the parent field in the listener is paired to the correct text box
        startListener = new RelativeTimeDocumentListener(this, start, start, end, days);
        endListener = new RelativeTimeDocumentListener(this, end, start, end, days);
        daysListener = new RelativeTimeDocumentListener(this, days, start, end, days);

        start.getDocument().addDocumentListener(startListener);
        end.getDocument().addDocumentListener(endListener);
        days.getDocument().addDocumentListener(daysListener);

        startListener.addPropertyChangeListener(this);
        endListener.addPropertyChangeListener(this);
        daysListener.addPropertyChangeListener(this);
    }

    private long getBaseTime() {
        return ((Date) base.getValue()).getTime();
    }

    @Override
    protected long getStartTime() {
        return getStartTime((Integer) start.getValue());
    }

    long getStartTime(int start) {
        return getBaseTime() + start * 1000L;
    }

    @Override
    long getEndTime() {
        return getEndTime((Integer) end.getValue(), (Integer) days.getValue());
    }

    long getEndTime(int end, int days) {
        return getBaseTime() + end * 1000L + days * 86400000L;
    }

    @Override
    void setTimes(long start, long end) {
        // set the base time so that the current start offset gives the correct time
        long base = start - (Integer) this.start.getValue() * 1000;

        if (end > start) {
            long diff = end - base;
            int numDays = (int) (diff / 86400000L);
            int endTime = (int) (diff / 1000 % 86400);

            days.setValue(numDays);
            this.end.setValue(endTime);
        }

        // set base last since it fires a ChangeEvent which needs the new end time
        this.base.setValue(new Date(base));
    }

    @Override
    TimeZone getTimeZone() {
        DateEditor de = (DateEditor) base.getEditor();
        return de.getFormat().getTimeZone();
    }

    /**
     * @param base epoch start time
     * @param start milliseconds < 1 day
     * @param end epoch end time
     */
    @Override
    protected void setStartToEnd() {
        start.setValue(end.getValue());
        requestFocus(start);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            requestFocus(start);
        }
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // document listeners should also propagate property changes to listeners
        startListener.addPropertyChangeListener(listener);
        endListener.addPropertyChangeListener(listener);
        daysListener.addPropertyChangeListener(listener);

        super.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // all but timeZone fired by IntervalDocumentListeners
        if ("interval".equals(evt.getPropertyName())) {
            if (evt.getNewValue() == null) {
                startLabel.setFont(Styles.LABEL_ERROR);
                endLabel.setFont(Styles.LABEL_ERROR);

                startLabel.setForeground(Styles.ERROR_COLOR);
                endLabel.setForeground(Styles.ERROR_COLOR);

                start.setForeground(Styles.ERROR_COLOR);
                end.setForeground(Styles.ERROR_COLOR);
                days.setForeground(Styles.ERROR_COLOR);
            }
            else {
                startLabel.setFont(Styles.LABEL);
                endLabel.setFont(Styles.LABEL);

                startLabel.setForeground(Styles.DEFAULT_COLOR);
                endLabel.setForeground(Styles.DEFAULT_COLOR);

                start.setForeground(Styles.DEFAULT_COLOR);
                end.setForeground(Styles.DEFAULT_COLOR);
                days.setForeground(Styles.DEFAULT_COLOR);
            }
        }
        else if ("start".equals(evt.getPropertyName())) {
            startAbsolute.setText(FORMAT.format(new Date((Long) evt.getNewValue())));
        }
        else if ("end".equals(evt.getPropertyName())) {
            endAbsolute.setText(FORMAT.format(new Date((Long) evt.getNewValue())));
        }
        else if ("timeZone".equals(evt.getPropertyName())) {
            TimeZone timeZone = (TimeZone) evt.getNewValue();

            DateEditor de = (DateEditor) base.getEditor();
            de.getFormat().setTimeZone(timeZone);

            // hack to get the spinner to fire a state change and update the displayed value
            // toggle the calendar field back to its original value
            ((SpinnerDateModel) base.getModel()).setCalendarField(Calendar.MINUTE);
            ((SpinnerDateModel) base.getModel()).setCalendarField(Calendar.SECOND);

            FORMAT.setTimeZone(timeZone);
            startAbsolute.setText(FORMAT.format(new Date(getStartTime())));
            endAbsolute.setText((FORMAT.format(new Date(getEndTime()))));
        }
    }
}