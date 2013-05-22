package com.ibm.nmon.gui.file;

import java.util.Map;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JButton;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListSelectionModel;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.GUITable;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.gui.table.ReadOnlyTableModel;

import com.ibm.nmon.gui.util.LogViewerDialog;
import com.ibm.nmon.gui.util.ScrollingTableFix;

final class ParserErrorDialog extends GUIDialog {
    private final Map<String, String> errors;

    private final JSplitPane splitPane;

    private final GUITable errorList;
    private final JTextArea errorText;
    private final JButton ok;

    public ParserErrorDialog(NMONVisualizerGui gui, Map<String, String> fileErrors) {
        super(gui, "Parsing Errors");
        setModal(true);

        setIconImage(LogViewerDialog.LOG_ICON.getImage());

        this.errors = fileErrors;

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(Styles.LOWER_LINE_BORDER);

        ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JButton copy = new JButton("Copy");
        copy.setIcon(Styles.COPY_ICON);
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReadOnlyTableModel model = (ReadOnlyTableModel) errorList.getModel();
                String filename = (String) model.getValueAt(errorList.getSelectedRow(), 0);

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(filename + ':' + '\n' + errorText.getText()), null);
            }
        });

        JButton copyAll = new JButton("Copy All");
        copyAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringBuilder allErrors = new StringBuilder(errors.size() * 256);
                ReadOnlyTableModel model = (ReadOnlyTableModel) errorList.getModel();

                for (int i = 0; i < model.getRowCount(); i++) {
                    if (i != 0) {
                        allErrors.append('\n');
                    }

                    String filename = (String) model.getValueAt(i, 0);
                    allErrors.append(filename);
                    allErrors.append(':');
                    allErrors.append('\n');
                    allErrors.append(errors.get(filename));
                }

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(allErrors.toString()), null);
            }
        });

        errorList = new GUITable(gui, new ReadOnlyTableModel());
        errorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // only one column, no need for a header
        errorList.setTableHeader(null);
        errorList.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    dispose();
                    e.consume();
                }
            }
        });

        // still need to add the column or the data will not show up
        ((ReadOnlyTableModel) errorList.getModel()).addColumn("");

        for (String filename : errors.keySet()) {
            ((ReadOnlyTableModel) errorList.getModel()).addRow(new Object[] { filename });
        }

        errorText = new JTextArea();
        errorText.setColumns(75);
        errorText.setRows(15);
        errorText.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        errorText.setEditable(false);

        ((DefaultListSelectionModel) errorList.getSelectionModel())
                .addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            String filename = (String) errorList.getModel().getValueAt(errorList.getSelectedRow(), 0);
                            errorText.setText(errors.get(filename));
                        }
                    }
                });

        JScrollPane scroller = new JScrollPane(errorList);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.addComponentListener(new ScrollingTableFix(errorList, scroller));
        scroller.setColumnHeaderView(null);

        splitPane.setTopComponent(scroller);

        scroller = new JScrollPane(errorText);
        scroller.setBorder(null);

        splitPane.setBottomComponent(scroller);

        add(splitPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        // temp panel so button does not expand
        JPanel temp = new JPanel();
        temp.add(ok);
        // footer.setBorder(Styles.CONTENT_BORDER);
        footer.add(temp, BorderLayout.LINE_END);

        temp = new JPanel();
        temp.add(copy);
        temp.add(copyAll);
        footer.add(temp, BorderLayout.LINE_START);

        add(footer, BorderLayout.PAGE_END);

        // 3/4 the height and 1/2 the width
        setPreferredSize(new java.awt.Dimension((int) (gui.getMainFrame().getWidth() * .75), (int) (gui.getMainFrame()
                .getHeight() * .5)));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // divider location must be set when the window is visible
                // because this dialog is modal, it cannot be done in setVisible() since
                // super.setVisible() has not been called
                splitPane.setDividerLocation(.25);
            }
        });
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            // used for visual effect only
            // does not do anything since table intercepts the Enter key press
            // added KeyListener recreates the correct behavior
            getRootPane().setDefaultButton(ok);

            // select the first error
            errorList.getSelectionModel().setSelectionInterval(0, 0);
        }

        super.setVisible(b);
    }
}
