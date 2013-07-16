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

final class ReportMenu extends JMenuBar {
    private final ReportFrame parent;

    private final ReportFileChooser chooser;

    public ReportMenu(ReportFrame parent) {
        super();

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
                    ReportMenu.this.parent.loadReportDefinition(chooser.getSelectedFile());
                }
            }
        });

        file.add(item);

        item = new JMenuItem("Save...");
        item.setMnemonic('s');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReportMenu.this.parent.saveAllCharts();
            }
        });

        file.add(item);
        file.addSeparator();

        item = new JMenuItem("Close");
        item.setMnemonic('c');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReportMenu.this.parent.dispose();
            }
        });

        file.add(item);

        add(file);

        JMenu help = new JMenu("Help");
        help.setMnemonic('h');

      //  help.add(item);

        add(help);
    }
}
