package com.ibm.nmon.gui.info;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;

import javax.swing.ImageIcon;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;

import javax.swing.SwingConstants;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.tree.TreePathParser;
import com.ibm.nmon.util.TimeFormatCache;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.ProcessDataType;

import com.ibm.nmon.data.Process;

public final class ProcessInfoDialog extends GUIDialog {
    private static final ImageIcon PROCESS_ICON = Styles.buildIcon("cog.png");

    private final JLabel processName;
    private final JLabel processTime;
    private final JTextArea commandLine;
    private final JCheckBox followTree;

    private JTree tree;

    public ProcessInfoDialog(NMONVisualizerGui gui) {
        super(gui);

        setLayout(new BorderLayout());
        setIconImage(PROCESS_ICON.getImage());

        processName = new JLabel();
        processName.setFont(Styles.TITLE);
        processName.setHorizontalAlignment(SwingConstants.CENTER);

        processTime = new JLabel();
        processTime.setFont(Styles.BOLD);
        processTime.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel header = new JPanel();
        header.setLayout(new GridLayout(2, 1));
        header.setBorder(Styles.TITLE_BORDER);
        header.add(processName);
        header.add(processTime);

        add(header, BorderLayout.PAGE_START);

        commandLine = new JTextArea();
        commandLine.setColumns(50);
        commandLine.setRows(15);
        commandLine.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(commandLine);
        scrollPane.setBorder(Styles.DOUBLE_LINE_BORDER);
        add(scrollPane, BorderLayout.CENTER);

        followTree = new JCheckBox("Link with Tree?");
        followTree.setFont(Styles.LABEL);
        followTree.setHorizontalAlignment(SwingConstants.TRAILING);
        followTree.setHorizontalTextPosition(SwingConstants.LEADING);
        followTree.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
        followTree.setSelected(false);

        add(followTree, BorderLayout.PAGE_END);

        tree = null;
    }

    public ProcessInfoDialog(NMONVisualizerGui gui, JTree tree, Process process) {
        this(gui);

        linkToTree(tree);
        setProcess(process);
    }

    public void linkToTree(JTree tree) {
        if (this.tree != null) {
            this.tree.removeTreeSelectionListener(treeListener);
        }

        this.tree = tree;

        if (tree != null) {
            tree.addTreeSelectionListener(treeListener);
            followTree.setSelected(true);
        }
        else {
            followTree.setSelected(false);
        }
    }

    @Override
    public void dispose() {
        if (this.tree != null) {
            this.tree.removeTreeSelectionListener(treeListener);
        }

        super.dispose();
    }

    private void setProcess(Process process) {
        setTitle(process.getName() + " (" + process.getId() + ')' + " Command Line");

        processName.setText(process.getName() + " (" + process.getId() + ')');
        processTime.setText(TimeFormatCache.formatDateTime(process.getStartTime()) + " - "
                + TimeFormatCache.formatDateTime(process.getEndTime()));

        String[] parts = process.getCommandLine().split("\\s");
        StringBuilder builder = new StringBuilder(process.getCommandLine().length() + 100);
        int maxWidth = Integer.MIN_VALUE;

        for (int i = 0; i < parts.length - 1; i++) {
            builder.append(parts[i]);
            builder.append('\n');

            if (parts[i].length() > maxWidth) {
                maxWidth = parts[i].length();
            }
        }

        builder.append(parts[parts.length - 1]);

        if (parts[parts.length - 1].length() > maxWidth) {
            maxWidth = parts[parts.length - 1].length();
        }

        commandLine.setText(builder.toString());
        commandLine.setCaretPosition(0);
    }

    private final TreeSelectionListener treeListener = new TreeSelectionListener() {
        private TreePathParser pathParser = new TreePathParser() {
            protected void onTypePath(DataSet data, DataType type) {
                if (followTree.isSelected() && type.getClass().equals(ProcessDataType.class)) {
                    Process p = ((ProcessDataType) type).getProcess();

                    if (p.getId() != -1) {
                        setProcess(p);
                    }
                }
            };
        };

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            pathParser.parse(e.getPath());
        }
    };
}
