package com.ibm.nmon.gui.tree;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.Process;

import com.ibm.nmon.gui.Styles;

final class TreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = -2417103407578950522L;

    private static final ImageIcon DATATYPE_ICON = Styles.buildIcon("package.png");
    private static final ImageIcon DATASUBTYPE_ICON = Styles.buildIcon("package_green.png");
    private static final ImageIcon FIELD_ICON = Styles.buildIcon("page.png");
    private static final ImageIcon PROCESS_ICON = Styles.buildIcon("cog.png");

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        Object o = ((DefaultMutableTreeNode) value).getUserObject();

        if (o instanceof DataSet) {
            setIcon(Styles.COMPUTER_ICON);
            setToolTipText(null);
        }
        else if (o instanceof DataType) {
            setIcon(DATATYPE_ICON);
            setToolTipText(((DataType) o).getName());
        }
        else if (o instanceof Process) {
            setIcon(PROCESS_ICON);

            // Show the command line on Process nodes
            // Truncate names longer than 100 characters
            String commandLine = ((Process) o).getCommandLine();

            if (commandLine.length() > 0) {
                if (commandLine.length() > 100) {
                    commandLine = commandLine.substring(0, 50) + " ... "
                            + commandLine.substring(commandLine.length() - 50);
                }

                setToolTipText(commandLine);
            }
            else {
                setToolTipText(((Process) o).getName());
            }
        }
        else if (o instanceof String) {
            String s = (String) o;

            if (s.equals(TreePanel.ROOT_NAME)) {
                setIcon(Styles.REPORT_ICON);
                setToolTipText(null);
            }
            else if (s.equals("TOP")) {
                setIcon(DATASUBTYPE_ICON);
                setToolTipText("Top Processes");
            }
            else if (s.equals("Process")) {
                setIcon(DATASUBTYPE_ICON);
                setToolTipText("Processes");
            }
            else if (s.equals("GC")) {
                setIcon(DATASUBTYPE_ICON);
                setToolTipText("Garbage Collection");
            }
            else {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) value).getParent();

                if (parent.getUserObject() instanceof DataSet) {
                    // sub data type
                    setIcon(DATASUBTYPE_ICON);

                    // set tool tip the same as the first child
                    setToolTipText(s);
                }
                else {
                    String p = parent.toString();

                    if (p.equals("TOP")) {
                        setIcon(DATATYPE_ICON);
                    }
                    if (p.equals("Process")) {
                        setIcon(DATATYPE_ICON);
                    }
                    else if (p.equals("GC")) {
                        setIcon(DATATYPE_ICON);
                    }
                    else {
                        setIcon(FIELD_ICON);
                    }

                    setToolTipText(null);
                }
            }
        }

        return this;
    }
}
