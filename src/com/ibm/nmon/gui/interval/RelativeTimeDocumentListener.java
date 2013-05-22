package com.ibm.nmon.gui.interval;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JFormattedTextField;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import com.ibm.nmon.gui.time.TimeMaskFormatter;

import com.ibm.nmon.interval.Interval;

/**
 * DocumentListener used by {@link RelativeTimeIntervalPanel} to ensure that changes in the entry
 * text boxes update the absolute time labels and the displayed interval.
 */
final class RelativeTimeDocumentListener implements DocumentListener {
    private final RelativeTimeIntervalPanel relativeTimeIntervalPanel;
    private final JFormattedTextField parent;

    private final JFormattedTextField start;
    private final JFormattedTextField end;
    private final JFormattedTextField days;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    RelativeTimeDocumentListener(RelativeTimeIntervalPanel relativeTimeIntervalPanel, JFormattedTextField parent,
            JFormattedTextField start, JFormattedTextField end, JFormattedTextField days) {

        this.relativeTimeIntervalPanel = relativeTimeIntervalPanel;

        this.parent = parent;

        this.start = start;
        this.end = end;
        this.days = days;
    }

    @Override
    public void removeUpdate(DocumentEvent e) {}

    @Override
    public void insertUpdate(DocumentEvent e) {
        if (e.getLength() == 0) {
            return;
        }

        try {
            String text = e.getDocument().getText(e.getDocument().getStartPosition().getOffset(),
                    e.getDocument().getLength());

            int startValue = -1;
            int endValue = -1;
            int daysValue = -1;

            String property = null;

            if (parent == start) {
                try {
                    startValue = TimeMaskFormatter.parseTime(text);
                }
                catch (NumberFormatException nfe) {}

                endValue = (Integer) end.getValue();
                daysValue = (Integer) days.getValue();

                property = "start";
            }
            else if (parent == end) {
                try {
                    endValue = TimeMaskFormatter.parseTime(text);
                }
                catch (NumberFormatException nfe) {}

                startValue = (Integer) start.getValue();
                daysValue = (Integer) days.getValue();

                property = "end";
            }
            else if (parent == days) {
                try {
                    daysValue = Integer.parseInt(text);
                }
                catch (NumberFormatException nfe) {}

                startValue = (Integer) start.getValue();
                endValue = (Integer) end.getValue();

                property = "end";
            }
            else {
                throw new IllegalStateException("unexpected parent " + parent);
            }

            if ((startValue != -1) && (endValue != -1) && (daysValue != -1)) {
                long startTime = relativeTimeIntervalPanel.getStartTime(startValue);
                long endTime = relativeTimeIntervalPanel.getEndTime(endValue, daysValue);

                if (startTime < endTime) {
                    propertyChangeSupport.firePropertyChange("interval", null, new Interval(startTime, endTime));
                }
                else {
                    propertyChangeSupport.firePropertyChange("interval", null, null);
                }

                if ("start".equals(property)) {
                    propertyChangeSupport.firePropertyChange(property, null, startTime);
                }
                else {
                    propertyChangeSupport.firePropertyChange(property, null, endTime);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {}

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}