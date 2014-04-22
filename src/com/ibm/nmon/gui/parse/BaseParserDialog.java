package com.ibm.nmon.gui.parse;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
 * Base dialog to get the for pre- & post-processing of files. Displays a hostname combo box and
 * allows the file to be skipped by default.
 * 
 * @see com.ibm.nmon.gui.main.NMONVisualizerGui#getDataForHATJParse(String)
 */
abstract class BaseParserDialog extends GUIDialog {
    private static final long serialVersionUID = 5488444964553569979L;

    private final JLabel parsedFileLabel;
    private final JComboBox<String> hostnames;

    private final JButton ok;

    private boolean skip = true;

    protected BaseParserDialog(NMONVisualizerGui gui, String title) {
        super(gui, gui.getMainFrame(), title);

        setModal(true);
        setResizable(false);
        setLayout(new BorderLayout());

        HostnameComboBoxModel model = new HostnameComboBoxModel(gui);

        hostnames = new JComboBox<String>(model);
        hostnames.setEditable(true);

        ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String hostname = getHostname();

                if ((hostname == null) || "".equals(hostname)) {
                    JOptionPane.showMessageDialog(BaseParserDialog.this, "Hostname" + " is required", "Missing Value",
                            JOptionPane.ERROR_MESSAGE);
                }
                else {
                    if (validateOK()) {
                        skip = false;

                        beforeDispose();
                        dispose();
                    }
                }
            }
        });

        JButton skip = new JButton("Skip");
        skip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BaseParserDialog.this.skip = true;
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

        addComponents(content, labelConstraints, fieldConstraints);

        JPanel temp = new JPanel();
        temp.add(ok);
        temp.add(skip);

        add(parsedFileLabel, BorderLayout.PAGE_START);
        add(content, BorderLayout.CENTER);
        add(temp, BorderLayout.PAGE_END);

        pack();
        setLocationRelativeTo(gui.getMainFrame());
    }

    protected void addComponents(JPanel content, GridBagConstraints labelConstraints,
            GridBagConstraints fieldConstraints) {}

    protected boolean validateOK() {
        return true;
    }

    protected void beforeDispose() {}

    public final void parseDataSet(String fileToParse) {
        int idx = fileToParse.lastIndexOf('/');

        if (idx != -1) {
            fileToParse = fileToParse.substring(idx + 1);
        }

        parsedFileLabel.setText(fileToParse);

        // modal dialog - setVisible() does not return until closed
        setVisible(true);
    }

    public final boolean isSkipped() {
        return skip;
    }

    public final String getHostname() {
        // HostnameComboBoxModel already trims the strings
        return (String) hostnames.getSelectedItem();
    }

    public final void setHostname(String hostname) {
        hostnames.setSelectedItem(hostname);
    }

    public final void setVisible(boolean b) {
        if (b) {
            skip = false;
            hostnames.requestFocus();
            getRootPane().setDefaultButton(ok);
        }

        super.setVisible(b);
    }
}
