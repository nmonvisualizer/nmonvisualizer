package com.ibm.nmon.gui.report;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import javax.swing.JFileChooser;

import com.ibm.nmon.gui.chart.annotate.AnnotationCache;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.util.GranularityDialog;
import com.ibm.nmon.gui.util.LogViewerDialog;

final class ReportMenu extends JMenuBar {
    private static final long serialVersionUID = 2244915433441666995L;

    private final NMONVisualizerGui gui;
    private final ReportFrame parent;

    private final ReportFileChooser chooser;

    private final JMenuItem save;

    ReportMenu(NMONVisualizerGui gui, ReportFrame parent) {
        super();

        this.gui = gui;
        this.parent = parent;
        this.chooser = new ReportFileChooser(parent.getGui());

        JMenu file = new JMenu("File");
        file.setMnemonic('f');

        JMenuItem item = new JMenuItem("Load...");
        item.setMnemonic('l');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (chooser.showDialog(ReportMenu.this.parent, "Parse") == JFileChooser.APPROVE_OPTION) {
                    boolean reportLoaded = ReportMenu.this.parent.loadReportDefinition(chooser.getSelectedFile());

                    save.setEnabled(reportLoaded);
                }
            }
        });

        file.add(item);

        JMenu loadDefaults = new JMenu("Load Default");
        loadDefaults.setMnemonic('d');
        file.add(loadDefaults);

        item = new JMenuItem("DataSet Report");
        item.setMnemonic('d');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReportMenu.this.parent.loadDefaultDataSetReport();
                save.setEnabled(false);
            }
        });

        loadDefaults.add(item);

        item = new JMenuItem("All Systems Report");
        item.setMnemonic('a');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReportMenu.this.parent.loadDefaultSummaryReport();
                save.setEnabled(false);
            }
        });

        loadDefaults.add(item);

        loadDefaults.addSeparator();

        item = new JMenuItem("IOStat Report");
        item.setMnemonic('i');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReportMenu.this.parent.loadDefaultIOStatReport();
                save.setEnabled(false);
            }
        });

        loadDefaults.add(item);

        item = new JMenuItem("IOStat Disk Data Report");
        item.setMnemonic('k');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReportMenu.this.parent.loadDefaultIOStatDiskDataReport();
                save.setEnabled(false);
            }
        });

        loadDefaults.add(item);

        save = new JMenuItem("Save...");
        save.setMnemonic('s');
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReportMenu.this.parent.saveAllCharts();
            }
        });
        // no report initially loaded
        save.setEnabled(false);

        file.add(save);
        file.addSeparator();

        item = new JMenuItem("Close");
        item.setMnemonic('c');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.CTRL_DOWN_MASK));

        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReportMenu.this.parent.dispose();
            }
        });

        file.add(item);

        add(file);

        JMenu view = new JMenu("View");
        view.setMnemonic('v');

        item = new JMenuItem("Set Granularity...");
        item.setMnemonic('g');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));

        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new GranularityDialog(ReportMenu.this.gui, ReportMenu.this.parent).setVisible(true);
            }
        });

        view.add(item);

        view.addSeparator();

        item = new JMenuItem("Clear Annotations");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationCache.clear();
            }
        });

        view.add(item);

        item = new JMenuItem("Remove Last Line");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationCache.removeLastMarker();
            }
        });

        view.add(item);

        item = new JMenuItem("Remove Last Text");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationCache.removeLastAnnotation();
            }
        });

        view.add(item);

        add(view);

        JMenu help = new JMenu("Help");
        help.setMnemonic('h');

        item = new JMenuItem("View Log...");
        item.setMnemonic('l');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        item.setIcon(LogViewerDialog.LOG_ICON);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!ReportMenu.this.gui.getLogViewer().isVisible()) {
                    ReportMenu.this.gui.getLogViewer().setVisible(true);
                }
                else {
                    ReportMenu.this.gui.getLogViewer().toFront();
                }

                ReportMenu.this.gui.getLogViewer().setLocationRelativeTo(ReportMenu.this.parent);
            }
        });

        help.add(item);

        add(help);
    }
}
