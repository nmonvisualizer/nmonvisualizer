package com.ibm.nmon.gui.parse;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import java.awt.GridBagConstraints;
import javax.swing.SwingConstants;

import javax.swing.DefaultComboBoxModel;

import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.ibm.nmon.gui.Styles;

/**
 * Dialog to get the JVM name and hostname for a particular verbose GC file.
 * 
 * @see com.ibm.nmon.gui.main.NMONVisualizerGui#getDataForGCParse(String)
 */
public final class VerboseGCPreParser extends BaseParserDialog {
    private static final long serialVersionUID = 647178802418910814L;

    private JComboBox<String> jvmNames;

    public VerboseGCPreParser(NMONVisualizerGui gui) {
        super(gui, "Missing GC Information");
    }

    @Override
    protected void addComponents(JPanel content, GridBagConstraints labelConstraints,
            GridBagConstraints fieldConstraints) {
        jvmNames = new JComboBox<String>();
        jvmNames.setEditable(true);

        JLabel nameLabel = new JLabel("JVM Name:");
        nameLabel.setFont(Styles.LABEL);
        nameLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        content.add(nameLabel, labelConstraints);
        content.add(jvmNames, fieldConstraints);
    }

    @Override
    protected boolean validateOK() {
        String jvmName = getJVMName();

        if ((jvmName == null) || "".equals(jvmName)) {
            JOptionPane.showMessageDialog(VerboseGCPreParser.this, "JVM Name" + " is required", "Missing Value",
                    JOptionPane.ERROR_MESSAGE);

            return false;
        }
        else {
            return true;
        }
    }

    @Override
    protected void beforeDispose() {
        boolean existing = false;
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) jvmNames.getModel();

        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).equals(jvmNames.getSelectedItem())) {
                existing = true;
                break;
            }
        }

        if (!existing) {
            model.addElement((String) jvmNames.getSelectedItem());
        }
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
}
