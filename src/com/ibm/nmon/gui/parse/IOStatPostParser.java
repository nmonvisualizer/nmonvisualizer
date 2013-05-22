package com.ibm.nmon.gui.parse;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Calendar;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import javax.swing.SwingConstants;
import javax.swing.JSpinner.DateEditor;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.gui.util.HostnameComboBoxModel;

import com.ibm.nmon.util.DataHelper;

/**
 * Dialog to get the JVM name and hostname for a particular verbose GC file.
 * 
 * @see com.ibm.nmon.gui.main.NMONVisualizerGui#getDataForGCParse(String)
 */
public final class IOStatPostParser extends GUIDialog {
    private final JLabel parsedFileLabel;
    private final JComboBox hostnames;
    private final JSpinner date;

    private final JButton ok;

    private boolean skip = true;

    public IOStatPostParser(NMONVisualizerGui gui) {
        super(gui, "Missing IOStat Information");

        setModal(true);
        setResizable(false);
        setLayout(new BorderLayout());

        HostnameComboBoxModel model = new HostnameComboBoxModel(gui);

        hostnames = new JComboBox(model);
        hostnames.setEditable(true);

        ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String hostname = getHostname();

                if ((hostname == null) || "".equals(hostname)) {
                    JOptionPane.showMessageDialog(IOStatPostParser.this, "Hostname" + " is required", "Missing Value",
                            JOptionPane.ERROR_MESSAGE);
                }
                else {
                    skip = false;

                    dispose();
                }
            }
        });

        JButton skip = new JButton("Skip");
        skip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IOStatPostParser.this.skip = true;
                dispose();
            }
        });

        parsedFileLabel = new JLabel();
        parsedFileLabel.setFont(Styles.TITLE);
        parsedFileLabel.setHorizontalAlignment(SwingConstants.CENTER);
        parsedFileLabel.setBorder(Styles.TITLE_BORDER);

        JLabel hostnameLabel = new JLabel("Hostname:");
        hostnameLabel.setFont(Styles.LABEL);
        hostnameLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(Styles.LABEL);
        dateLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        date = new JSpinner(new SpinnerDateModel(new Date(DataHelper.today()), null, null, Calendar.DAY_OF_WEEK));
        date.setEditor(new DateEditor(date, "MMM dd yyyy"));

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(Styles.CONTENT_BORDER);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        Insets insets = new Insets(5, 0, 0, 5);

        labelConstraints.gridx = 0;
        fieldConstraints.gridx = 1;

        labelConstraints.insets = insets;
        fieldConstraints.insets = insets;

        labelConstraints.anchor = GridBagConstraints.BASELINE_TRAILING;
        fieldConstraints.anchor = GridBagConstraints.BASELINE_LEADING;

        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.fill = GridBagConstraints.REMAINDER;

        labelConstraints.gridy = 0;
        fieldConstraints.gridy = 0;

        content.add(hostnameLabel, labelConstraints);
        content.add(hostnames, fieldConstraints);

        labelConstraints.gridy = 1;
        fieldConstraints.gridy = 1;

        content.add(dateLabel, labelConstraints);
        content.add(date, fieldConstraints);

        JPanel temp = new JPanel();
        temp.add(ok);
        temp.add(skip);

        add(parsedFileLabel, BorderLayout.PAGE_START);
        add(content, BorderLayout.CENTER);
        add(temp, BorderLayout.PAGE_END);

        pack();
        setLocationRelativeTo(gui.getMainFrame());
    }

    public void postParseDataSet(String fileToParse) {
        int idx = fileToParse.lastIndexOf('/');

        if (idx != -1) {
            fileToParse = fileToParse.substring(idx + 1);
        }

        parsedFileLabel.setText(fileToParse);

        // modal dialog - setVisible() does not return until closed
        setVisible(true);
    }

    public boolean isSkipped() {
        return skip;
    }

    public String getHostname() {
        // HostnameComboBoxModel already trims the strings
        return (String) hostnames.getSelectedItem();
    }

    public void setHostname(String hostname) {
        hostnames.setSelectedItem(hostname);
    }

    public long getDate() {
        return ((Date) date.getValue()).getTime();
    }

    public void setVisible(boolean b) {
        if (b) {
            skip = false;
            hostnames.requestFocus();
            getRootPane().setDefaultButton(ok);
        }

        super.setVisible(b);
    }
}
