package com.ibm.nmon.gui.info;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Map;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.GUITable;
import com.ibm.nmon.gui.Styles;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;

import com.ibm.nmon.data.SystemDataSet;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.gui.table.ReadOnlyTableModel;
import com.ibm.nmon.gui.table.StringCellRenderer;

/**
 * Display SystemDataSet metadata information in a table. Supports copying rows to the clipboard.
 */
public final class MetadataInfoDialog extends GUIDialog implements PropertyChangeListener {
    private final SimpleDateFormat format = new SimpleDateFormat(Styles.DATE_FORMAT_STRING);

    private final JTabbedPane tabs;

    public MetadataInfoDialog(NMONVisualizerGui gui, SystemDataSet data) {
        super(gui, "Parsed File Info - " + data.getHostname());

        setIconImage(Styles.buildIcon("page.png").getImage());

        format.setTimeZone(gui.getDisplayTimeZone());

        if (data.getMetadataCount() > 1) {
            tabs = new JTabbedPane();

            setContentPane(tabs);

            ToolTipManager.sharedInstance().registerComponent(tabs);
        }
        else {
            tabs = null;
        }

        JPanel tab = null;

        for (long time : data.getSourceFileTimes()) {
            tab = new JPanel(new BorderLayout());

            ReadOnlyTableModel model = new ReadOnlyTableModel();

            // each name value pair gets a row in the table
            model.addColumn("Name");
            model.addColumn("Value");

            Map<String, String> metadata = data.getMetadata(time);

            if (metadata.isEmpty()) {
                continue;
            }

            for (String name : metadata.keySet()) {
                model.addRow(new String[] { name, metadata.get(name) });
            }

            final GUITable table = new GUITable(gui, model);

            javax.swing.table.TableColumnModel columnModel = table.getColumnModel();
            columnModel.getColumn(0).setPreferredWidth(150);
            columnModel.getColumn(1).setPreferredWidth(350);
            columnModel.getColumn(0).setCellRenderer(new StringCellRenderer());

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.getViewport().setBackground(java.awt.Color.WHITE);
            scrollPane.setBorder(Styles.LOWER_LINE_BORDER);

            tab.add(scrollPane, BorderLayout.CENTER);

            JButton button = new JButton("Copy All");
            button.setIcon(Styles.COPY_ICON);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    table.selectAll();
                    table.getTransferHandler().exportToClipboard(table,
                            Toolkit.getDefaultToolkit().getSystemClipboard(), TransferHandler.COPY);
                    table.removeRowSelectionInterval(0, table.getRowCount() - 1);
                }
            });

            JPanel panel = new JPanel();
            panel.add(button);

            tab.add(panel, java.awt.BorderLayout.PAGE_END);

            if (data.getMetadataCount() > 1) {
                tabs.add(format.format(new java.util.Date(time)), tab);
                tabs.setToolTipTextAt(tabs.getTabCount() - 1, data.getSourceFile(time));
            }
        }

        if (data.getMetadataCount() == 1) {
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
