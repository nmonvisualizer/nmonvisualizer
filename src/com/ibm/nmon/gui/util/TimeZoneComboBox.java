package com.ibm.nmon.gui.util;

import java.util.TimeZone;

import java.util.Vector;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;

import javax.swing.DefaultListCellRenderer;

import javax.swing.JComboBox;
import javax.swing.JList;

import com.ibm.nmon.util.TimeZoneFactory;

/**
 * Combo box that displays all timezones defined by {@link TimeZoneFactory}.
 */
public final class TimeZoneComboBox extends JComboBox {
    // use a vector here as a small memory savings since DefaultComboBoxModel does not copy the
    // vector but it would create a new Vector internally if passed an array of Strings
    private static final Vector<TimeZone> TIMEZONES = new Vector<TimeZone>(TimeZoneFactory.TIMEZONES);

    public TimeZoneComboBox(TimeZone timeZone) {
        super(new DefaultComboBoxModel(TIMEZONES));

        setRenderer(timeZoneListRenderer);
        setSelectedItem(timeZone);
    }

    public TimeZone getSelectedTimeZone() {
        return (TimeZone) getSelectedItem();
    }

    private static final DefaultListCellRenderer timeZoneListRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            setText(((TimeZone) value).getID());

            return c;
        }
    };
}
