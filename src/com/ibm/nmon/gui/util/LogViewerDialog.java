package com.ibm.nmon.gui.util;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.util.BasicFormatter;

public final class LogViewerDialog extends JFrame {
    private static final long serialVersionUID = 2036301168126084250L;

    public static final ImageIcon LOG_ICON = Styles.buildIcon("page_error.png");

    private final JComboBox<Level> levels;
    private JTextArea log;

    public LogViewerDialog(NMONVisualizerGui gui) {
        super("Application Log");

        setResizable(true);
        setIconImage(LOG_ICON.getImage());

        // there should only be one instance of this class per application
        // hide this dialog instead of disposing
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        log = new JTextArea();
        log.setColumns(100);
        log.setRows(30);
        log.setEditable(false);

        final JScrollPane scroller = new JScrollPane(log);
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
            private static final long serialVersionUID = 2136102234176694095L;

            @Override
            public void actionPerformed(ActionEvent e) {
                log = new JTextArea();
                log.setColumns(100);
                log.setRows(30);
                log.setEditable(false);
                
                scroller.setViewportView(log);
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
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "clear");

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

        pack();
        
        final LogHandler logHandler = configureLogging();

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                logHandler.setVisible(true);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                logHandler.setVisible(false);
            }
        });
    }

    protected JRootPane createRootPane() {
        JRootPane rootPane = super.createRootPane();

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "ESCAPE");

        rootPane.getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = 5405906817147819455L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        return rootPane;
    }

    private LogHandler configureLogging() {
        Logger root = Logger.getLogger("");

        levels.setSelectedItem(root.getLevel());

        // remove all existing handlers and add one that logs to this class
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        
        LogHandler logHandler = new LogHandler();

        root.addHandler(logHandler);
        
        return logHandler;
    }
    
    private class LogHandler extends Handler {
        private boolean visible = false;
        private StringBuilder pending = new StringBuilder();
        
        LogHandler() {
            setFormatter(new BasicFormatter());
        }

        @Override
        public void publish(LogRecord record) {
            if (isLoggable(record)) {
                synchronized (this) {
                    String str = getFormatter().format(record);
                    if (visible) {
                        SwingUtilities.invokeLater(new QueuedLog(str));
                    } else {
                        pending.append(str);
                    }
                }
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
        
        public void setVisible(boolean visible) {
            this.visible = visible;
            
            synchronized (this) {
                if (visible && pending.length() > 0) {
                    SwingUtilities.invokeLater(new QueuedLog(pending.toString()));
                    pending.setLength(0);
                }
            }
        }
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
