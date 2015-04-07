package com.ibm.nmon.gui.info;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.Styles;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ToolTipManager;

import com.ibm.nmon.data.SystemDataSet;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.interval.Interval;

/**
 * Display SystemDateSet system information for a given file. Supports copying data to the
 * clipboard.
 */
public final class SystemInfoDialog extends GUIDialog implements PropertyChangeListener {
    private static final long serialVersionUID = -5471229750755128948L;

    private final SimpleDateFormat format = new SimpleDateFormat(Styles.DATE_FORMAT_STRING);

    private final JTabbedPane tabs;

    public SystemInfoDialog(NMONVisualizerGui gui, SystemDataSet data) {
        super(gui, gui.getMainFrame(), "System Info - " + data.getHostname());

        setIconImage(Styles.buildIcon("computer.png").getImage());

        format.setTimeZone(gui.getDisplayTimeZone());

        if (data.getSystemInfoCount() > 1) {
            tabs = new JTabbedPane();
            tabs.setBorder(Styles.LOWER_LINE_BORDER);

            setContentPane(tabs);

            ToolTipManager.sharedInstance().registerComponent(tabs);
        }
        else {
            tabs = null;
        }

        JPanel tab = null;

        for (Interval interval : data.getSourceFileIntervals()) {
            tab = new JPanel(new BorderLayout());
            tab.setBorder(Styles.LOWER_LINE_BORDER);

            // create a non-editable text area for each piece of information and put them into a set
            // of tabs
            final JTabbedPane subTabs = new JTabbedPane();
            // subTabs.setBorder(null);

            Map<String, String> systemInfo = data.getSystemInfo(interval.getStart());

            if (systemInfo.isEmpty()) {
                continue;
            }

            for (String name : systemInfo.keySet()) {
                JTextArea textArea = new JTextArea(systemInfo.get(name));
                textArea.setColumns(50);
                textArea.setRows(25);
                textArea.setEditable(false);

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setBorder(null);

                subTabs.addTab(name, scrollPane);
            }

            tab.add(subTabs, BorderLayout.CENTER);

            JButton button = new JButton("Copy All");
            button.setIcon(Styles.COPY_ICON);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JScrollPane scrollPane = (JScrollPane) subTabs.getSelectedComponent();
                    JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();

                    // select all resets the cursor position, so save and restore it
                    int n = textArea.getCaretPosition();
                    textArea.selectAll();
                    textArea.copy();
                    textArea.setCaretPosition(n);
                }
            });

            JPanel panel = new JPanel();
            panel.add(button);

            tab.add(panel, java.awt.BorderLayout.PAGE_END);

            if (data.getSystemInfoCount() > 1) {
                tabs.add(format.format(new java.util.Date(interval.getStart())), tab);
                tabs.setToolTipTextAt(tabs.getTabCount() - 1, data.getSourceFile(interval));
            }
        }

        if (data.getSystemInfoCount() == 1) {
            setContentPane(tab);
        }

        gui.addPropertyChangeListener("timeZone", this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (tabs != null) {
            java.util.Date[] dates = new java.util.Date[tabs.getTabCount()];

            for (int i = 0; i < tabs.getTabCount(); i++) {
                try {
                    dates[i] = format.parse(tabs.getTitleAt(i));
                }
                catch (java.text.ParseException pe) {
                    // should never happen since the titles were set using the same format
                }
            }

            format.setTimeZone((java.util.TimeZone) evt.getNewValue());

            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setTitleAt(i, format.format(dates[i]));
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        gui.removePropertyChangeListener("timeZone", this);
    }
}
