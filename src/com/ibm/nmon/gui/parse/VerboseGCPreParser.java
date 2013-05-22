package com.ibm.nmon.gui.parse;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javax.swing.SwingConstants;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.util.HostnameComboBoxModel;

/**
 * Dialog to get the JVM name and hostname for a particular verbose GC file.
 * 
 * @see com.ibm.nmon.gui.main.NMONVisualizerGui#getDataForGCParse(String)
 */
public final class VerboseGCPreParser extends GUIDialog {
    private final JLabel parsedFileLabel;
    private final JComboBox hostnames;
    private final JComboBox jvmNames;

    private final JButton ok;

    private boolean skip = true;

    public VerboseGCPreParser(NMONVisualizerGui gui) {
        super(gui, "Missing GC Information");

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
                String jvmName = getJVMName();

                if ((hostname == null) || "".equals(hostname)) {
                    JOptionPane.showMessageDialog(VerboseGCPreParser.this, "Hostname" + " is required",
                            "Missing Value", JOptionPane.ERROR_MESSAGE);
                }
                else if ((jvmName == null) || "".equals(jvmName)) {
                    JOptionPane.showMessageDialog(VerboseGCPreParser.this, "JVM Name" + " is required",
                            "Missing Value", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    skip = false;

                    boolean existing = false;
                    DefaultComboBoxModel model = (DefaultComboBoxModel) jvmNames.getModel();

                    for (int i = 0; i < model.getSize(); i++) {
                        if (model.getElementAt(i).equals(jvmNames.getSelectedItem())) {
                            existing = true;
                            break;
                        }
                    }

                    if (!existing) {
                        model.addElement(jvmNames.getSelectedItem());
                    }

                    dispose();
                }
            }
        });

        JButton skip = new JButton("Skip");
        skip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VerboseGCPreParser.this.skip = true;
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

        JLabel nameLabel = new JLabel("JVM Name:");
        nameLabel.setFont(Styles.LABEL);
        nameLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        jvmNames = new JComboBox();
        jvmNames.setEditable(true);

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

        content.add(nameLabel, labelConstraints);
        content.add(jvmNames, fieldConstraints);

        JPanel temp = new JPanel();
        temp.add(ok);
        temp.add(skip);

        add(parsedFileLabel, BorderLayout.PAGE_START);
        add(content, BorderLayout.CENTER);
        add(temp, BorderLayout.PAGE_END);

        pack();
        setLocationRelativeTo(gui.getMainFrame());
    }

    public void preParseDataSet(String fileToParse) {
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

    public String getJVMName() {
        String name = (String) jvmNames.getSelectedItem();

        if (name == null) {
            name = "";
        }

        return name.trim();
    }

    public void setJVMName(String jvmName) {
        jvmNames.setSelectedItem(jvmName);
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
