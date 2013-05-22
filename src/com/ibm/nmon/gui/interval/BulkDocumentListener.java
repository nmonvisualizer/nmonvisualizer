package com.ibm.nmon.gui.interval;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JFormattedTextField;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import com.ibm.nmon.gui.time.TimeMaskFormatter;

import com.ibm.nmon.interval.Interval;

/**
 * DocumentListener used by {@link BulkTimeIntervalPanel} to ensure that changes in the entry text
 * boxes update the absolute time labels and the displayed interval.
 */
final class BulkDocumentListener implements DocumentListener {
    private final BulkIntervalPanel bulkIntervalPanel;
    private final JFormattedTextField parent;
    private final JFormattedTextField duration;
    private final JFormattedTextField days;
    private final JFormattedTextField repeat;
    private final JFormattedTextField offset;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    BulkDocumentListener(BulkIntervalPanel bulkIntervalPanel, JFormattedTextField parent, JFormattedTextField duration,
            JFormattedTextField days, JFormattedTextField repeat, JFormattedTextField offset) {

        this.bulkIntervalPanel = bulkIntervalPanel;

        this.parent = parent;

        this.duration = duration;
        this.days = days;
        this.repeat = repeat;
        this.offset = offset;
    }

    @Override
    public final void removeUpdate(DocumentEvent e) {}

    @Override
    public final void insertUpdate(DocumentEvent e) {
        if (e.getLength() == 0) {
            return;
        }

        String text = null;

        try {
            text = e.getDocument().getText(e.getDocument().getStartPosition().getOffset(), e.getDocument().getLength());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        int durationValue = -1;
        int daysValue = -1;
        int repeatValue = -1;
        int offsetValue = -1;

        if (e.getDocument() == duration.getDocument()) {
            try {
                durationValue = TimeMaskFormatter.parseTime(text);
            }
            catch (NumberFormatException nfe) {}

            daysValue = (Integer) days.getValue();
            repeatValue = (Integer) repeat.getValue();
            offsetValue = (Integer) offset.getValue();
        }
        else if (e.getDocument() == days.getDocument()) {
            try {
                daysValue = Integer.parseInt(text);
            }
            catch (NumberFormatException nfe) {}

            durationValue = (Integer) duration.getValue();
            repeatValue = (Integer) repeat.getValue();
            offsetValue = (Integer) offset.getValue();
        }
        else if (e.getDocument() == repeat.getDocument()) {
            try {
                repeatValue = Integer.parseInt(text);
            }
            catch (NumberFormatException nfe) {}

            durationValue = (Integer) duration.getValue();
            daysValue = (Integer) days.getValue();
            offsetValue = (Integer) offset.getValue();
        }
        else if (e.getDocument() == offset.getDocument()) {
            try {
                offsetValue = TimeMaskFormatter.parseTime(text);
            }
            catch (NumberFormatException nfe) {}

            durationValue = (Integer) duration.getValue();
            daysValue = (Integer) days.getValue();
            repeatValue = (Integer) repeat.getValue();
        }
        else {
            throw new IllegalStateException("unexpected parent " + parent);
        }

        long[] updatedValues = new long[5];
        updatedValues[0] = durationValue;
        updatedValues[1] = daysValue;
        updatedValues[2] = repeatValue;
        updatedValues[3] = offsetValue;
        updatedValues[4] = -1;

        if ((daysValue != -1) && (repeatValue != -1) && (durationValue != -1) && (offsetValue != -1)) {
            long startTime = bulkIntervalPanel.getStartTime();
            long endTime = bulkIntervalPanel.getEndTime(startTime, durationValue, daysValue, repeatValue, offsetValue);

            if (startTime < endTime) {
                updatedValues[4] = endTime;
                Interval i = new Interval(startTime, endTime);

                propertyChangeSupport.firePropertyChange("interval", null, i);
            }
            else {
                propertyChangeSupport.firePropertyChange("interval", null, null);
            }
        }

        propertyChangeSupport.firePropertyChange("values", null, updatedValues);
    }

    @Override
    public final void changedUpdate(DocumentEvent e) {}

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
