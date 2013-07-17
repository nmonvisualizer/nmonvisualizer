package com.ibm.nmon.gui.main;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.BorderFactory;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.SwingConstants;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.Styles;

/**
 * Modal JDialog to set the current granularity. Automatic granularity is also supported.
 * 
 * @see NMONVisualizerGui#setGranularity(int)
 */
final class GranularityDialog extends GUIDialog {
    private final JCheckBox automatic;
    private final JTextField granularity;

    private final JButton ok;

    public GranularityDialog(NMONVisualizerGui gui, JFrame parent) {
        super(gui, parent, " Granularity?");

        setLayout(new BorderLayout());
        setModal(true);

        automatic = new JCheckBox();
        automatic.setHorizontalAlignment(SwingConstants.TRAILING);
        automatic.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // when the checkbox is deselected, move focus to the textbox
                granularity.setEnabled(!automatic.isSelected());

                if (granularity.isEnabled()) {
                    granularity.requestFocus();
                    granularity.selectAll();
                }
            }
        });

        automatic.setSelected(gui.getBooleanProperty("automaticGranularity"));

        granularity = new JTextField(Integer.toString(gui.getGranularity() / 1000));
        granularity.setEnabled(!automatic.isSelected());
        granularity.setColumns(3);

        ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // granularity = -1 => automatic
                int newGranularity = -1;

                if (!GranularityDialog.this.automatic.isSelected()) {
                    try {
                        newGranularity = Integer.parseInt(GranularityDialog.this.granularity.getText()) * 1000;
                    }
                    catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(GranularityDialog.this.gui.getMainFrame(), "Granularity"
                                + " must be " + "a whole number!", "Granularity", JOptionPane.ERROR_MESSAGE);

                        return;
                    }

                    if (newGranularity < 1) {
                        JOptionPane.showMessageDialog(GranularityDialog.this.gui.getMainFrame(), "Granularity"
                                + " must be " + "greater than 0!", "Granularity", JOptionPane.ERROR_MESSAGE);

                        return;
                    }
                }

                GranularityDialog.this.gui.setGranularity(newGranularity);
                GranularityDialog.this.dispose();
            }
        });

        // pull question icon from JOptionPane
        JLabel icon = new JLabel((Icon) UIManager.get("OptionPane.questionIcon"));
        icon.setVerticalAlignment(SwingUtilities.TOP);
        icon.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 25));

        add(icon, BorderLayout.LINE_START);

        JPanel temp = new JPanel();
        temp.add(ok);

        add(temp, BorderLayout.PAGE_END);

        JLabel autoLabel = new JLabel("Automatic:");
        autoLabel.setFont(Styles.LABEL);
        autoLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        JLabel granularityLabel = new JLabel("Granularity:");
        granularityLabel.setFont(Styles.LABEL);
        granularityLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        JLabel seconds = new JLabel("sec");
        seconds.setFont(Styles.LABEL);

        JPanel options = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 0, 0, 2);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridy = 0;

        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        options.add(autoLabel, constraints);

        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        options.add(automatic, constraints);

        constraints.gridy = 1;

        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
        options.add(granularityLabel, constraints);

        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        options.add(granularity, constraints);

        constraints.gridx = 2;
        constraints.insets = new Insets(5, 0, 0, 5);
        options.add(seconds, constraints);

        add(options, BorderLayout.CENTER);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            getRootPane().setDefaultButton(ok);

            if (granularity.isEnabled()) {
                granularity.requestFocus();
                granularity.selectAll();
            }
        }

        super.setVisible(b);
    }
}
