package com.ibm.nmon.gui.parse;

import java.awt.GridBagConstraints;

import java.util.Calendar;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.SwingConstants;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.JSpinner.DateEditor;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.util.TimeHelper;

/**
 * Dialog to get the hostname and time zone for a particular IOStat file.
 * 
 * @see com.ibm.nmon.gui.main.NMONVisualizerGui#getDataForIOStatParse(String, String)
 */
public final class IOStatPostParser extends BaseParserDialog {
    private static final long serialVersionUID = -5466944320034595370L;

    private JSpinner date;

    public IOStatPostParser(NMONVisualizerGui gui) {
        super(gui, "Missing IOStat Information");
    }

    protected void addComponents(JPanel content, GridBagConstraints labelConstraints,
            GridBagConstraints fieldConstraints) {

        date = new JSpinner(new SpinnerDateModel(new Date(TimeHelper.today()), null, null, Calendar.DAY_OF_WEEK));
        date.setEditor(new DateEditor(date, "MMM dd yyyy"));

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(Styles.LABEL);
        dateLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        content.add(dateLabel, labelConstraints);
        content.add(date, fieldConstraints);
    }

    public long getDate() {
        return ((Date) date.getValue()).getTime();
    }
}
