package com.ibm.nmon.gui.main;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JRadioButton;

import javax.swing.ButtonGroup;

import com.ibm.nmon.gui.Styles;

/**
 * JPanel that displays a radio button toggle between 'Charts' and 'Table'. Used to display charts
 * or the summary table in the UI. Sets the <code>chartsDisplayed<code> property when toggled.
 * 
 * @see ViewManager
 */
final class ChartTableToggle extends JPanel implements PropertyChangeListener {
    private final NMONVisualizerGui gui;

    private final JRadioButton charts;
    private final JRadioButton table;

    public ChartTableToggle(NMONVisualizerGui gui) {
        // border layout pads differently than the default flow layout
        // use it so the text for the radio buttons lines up with other text in the parent
        super(new BorderLayout());

        this.gui = gui;

        charts = new JRadioButton("Charts");
        table = new JRadioButton("Table");

        charts.setFont(Styles.LABEL);
        table.setFont(Styles.LABEL);

        charts.setBorder(Styles.CONTENT_BORDER);
        table.setBorder(Styles.CONTENT_BORDER);

        charts.setActionCommand("Charts");
        table.setActionCommand("Table");

        ActionListener toggle = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChartTableToggle.this.gui.setProperty("chartsDisplayed", !e.getActionCommand().equals("Table"));
            }
        };

        charts.addActionListener(toggle);
        table.addActionListener(toggle);

        ButtonGroup group = new ButtonGroup();
        group.add(charts);
        group.add(table);

        charts.setSelected(true);
        table.setSelected(false);

        add(charts, BorderLayout.LINE_START);
        add(table, BorderLayout.LINE_END);

        gui.addPropertyChangeListener("chartsDisplayed", this);
    }

    // this class must also be a property change listener because the gui may change the display
    // mode in other ways (i.e. keyboard shortcut)
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("chartsDisplayed".equals(evt.getPropertyName())) {
            boolean chartsDisplayed = (Boolean) evt.getNewValue();

            if (chartsDisplayed) {
                charts.setSelected(true);
            }
            else {
                table.setSelected(true);
            }
        }
    }
}
