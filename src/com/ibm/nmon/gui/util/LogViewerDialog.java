package com.ibm.nmon.gui.util;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.logging.Handler;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

import javax.swing.ImageIcon;

import javax.swing.SwingUtilities;

import com.ibm.nmon.gui.GUIDialog;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.util.BasicFormatter;

// TODO make this a JFrame
public final class LogViewerDialog extends GUIDialog {
    public static final ImageIcon LOG_ICON = Styles.buildIcon("page_error.png");

    private final JComboBox<Level> levels;
    private final JTextArea log;

    public LogViewerDialog(NMONVisualizerGui gui) {
        super(gui, gui.getMainFrame(), "Application Log");

        setResizable(true);
        setIconImage(LOG_ICON.getImage());

        // there should only be one instance of this class per application
        // hide this dialog instead of disposing
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        log = new JTextArea();
        log.setColumns(100);
        log.setRows(30);
        log.setEditable(false);

        JScrollPane scroller = new JScrollPane(log);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setBorder(Styles.DOUBLE_LINE_BORDER);

        JLabel logLevel = new JLabel("Log Level:");
        logLevel.setFont(Styles.LABEL);
        // logLevel.setBorder(Styles.CONTENT_BORDER);

        levels = new JComboBox<Level>(new Level[] { Level.SEVERE, Level.WARNING, Level.INFO, Level.FINE, Level.FINEST,
                Level.OFF });
        levels.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // note, not setting the root logger level
                // all 3rd party logging will remain set to the default
                Logger.getLogger("com.ibm.nmon").setLevel((Level) e.getItem());
            }
        });

        AbstractAction clearAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                log.setText("");
            }
        };

        JButton clear = new JButton("Clear");
        clear.setIcon(Styles.CLEAR_ICON);
        clear.addActionListener(clearAction);

        JButton copyAll = new JButton("Copy All");
        copyAll.setIcon(Styles.COPY_ICON);
        copyAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.selectAll();
                log.getTransferHandler().exportToClipboard(log, Toolkit.getDefaultToolkit().getSystemClipboard(),
                        TransferHandler.COPY);
            }
        });

        ((JComponent) getComponent(0)).getActionMap().put("clear", clearAction);
        ((JComponent) getComponent(0)).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK), "clear");

        JPanel header = new JPanel();
        header.add(logLevel);
        header.add(levels);

        // use temp panels to keep buttons from expanding to fill the entire area
        JPanel footer = new JPanel(new BorderLayout());
        JPanel temp = new JPanel();
        temp.add(clear);
        footer.add(temp, BorderLayout.CENTER);

        temp = new JPanel();
        temp.add(copyAll);
        footer.add(temp, BorderLayout.LINE_START);

        // add a spacer to ensure the clear button is actually centered
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(copyAll.getPreferredSize());
        temp = new JPanel();
        temp.add(spacer);
        footer.add(temp, BorderLayout.LINE_END);

        add(header, BorderLayout.PAGE_START);
        add(scroller, BorderLayout.CENTER);
        add(footer, BorderLayout.PAGE_END);

        configureLogging();
    }

    private void configureLogging() {
        Logger root = Logger.getLogger("");

        levels.setSelectedItem(root.getLevel());

        // remove all existing handlers and add one that logs to this class
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        root.addHandler(new LogHandler());
    }

    private class LogHandler extends Handler {
        LogHandler() {
            setFormatter(new BasicFormatter());
        }

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                SwingUtilities.invokeLater(new QueuedLog(getFormatter().format(record)));
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }

    // helper class to ensure log events are added to the text area in the Swing event thread
    private class QueuedLog implements Runnable {
        private final String toLog;

        QueuedLog(String toLog) {
            this.toLog = toLog;
        }

        @Override
        public void run() {
            log.append(toLog);
        }
    }
}
