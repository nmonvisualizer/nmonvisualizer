package com.ibm.nmon.gui.data;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javax.swing.SwingConstants;

import com.ibm.nmon.data.SystemDataSet;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.util.HostnameComboBoxModel;

/**
 * Modal JDialog to merge a given {@link SystemDataSet} into another. This allows multiple datasets
 * parsed under different hostnames to be merged into a single dataset in the GUI.
 * 
 * @see SystemDataSet#addData(SystemDataSet)
 */
public final class MergeDataSetDialog extends GUIDialog {
    private JComboBox<String> hostnames;
    private JButton merge;
    private SystemDataSet toMerge;

    public MergeDataSetDialog(NMONVisualizerGui gui, SystemDataSet toMerge) {
        super(gui, gui.getMainFrame(), "Merge With?");

        setResizable(false);
        setModal(true);

        this.toMerge = toMerge;

        JLabel hostname = new JLabel(toMerge.getHostname());
        hostname.setFont(Styles.TITLE);
        hostname.setHorizontalAlignment(SwingConstants.CENTER);
        hostname.setBorder(Styles.TITLE_BORDER);

        JLabel mergeHostLabel = new JLabel("Merge Into:");
        mergeHostLabel.setFont(Styles.LABEL);

        hostnames = new JComboBox<String>(new HostnameComboBoxModel(gui));
        hostnames.setEditable(false);

        merge = new JButton("Merge");
        merge.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String hostname = (String) hostnames.getSelectedItem();

                if ("".equals(hostname)) {
                    JOptionPane.showMessageDialog(MergeDataSetDialog.this, "Please select a valid hostname",
                            "Invalid hostname", JOptionPane.ERROR_MESSAGE);
                }
                else if (MergeDataSetDialog.this.toMerge.getHostname().equals(hostname)) {
                    dispose();
                }
                else {
                    for (SystemDataSet data : MergeDataSetDialog.this.gui.getDataSets()) {
                        if (data.getHostname().equals(hostname)) {
                            data.addData(MergeDataSetDialog.this.toMerge);
                            MergeDataSetDialog.this.gui.removeDataSet(MergeDataSetDialog.this.toMerge);
                            MergeDataSetDialog.this.gui.updateDataSet(data);
                            dispose();
                            return;
                        }
                    }
                }
            }
        });

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JPanel center = new JPanel();
        center.setBorder(Styles.CONTENT_BORDER);
        center.add(mergeHostLabel);
        center.add(hostnames);

        JPanel temp = new JPanel();
        temp.add(merge);
        temp.add(cancel);

        add(hostname, BorderLayout.PAGE_START);
        add(center, BorderLayout.CENTER);
        add(temp, BorderLayout.PAGE_END);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            getRootPane().setDefaultButton(merge);

            hostnames.setSelectedItem(hostnames.getItemAt(0));
            hostnames.requestFocus();
        }

        super.setVisible(b);
    }
}
