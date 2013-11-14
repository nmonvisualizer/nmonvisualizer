package com.ibm.nmon.gui.tree;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import java.awt.datatransfer.StringSelection;

import java.io.IOException;
import java.io.StringWriter;

import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.gui.data.RemoveAllDataSetsAction;
import com.ibm.nmon.gui.file.FileLoadAction;

import com.ibm.nmon.gui.data.MergeDataSetDialog;
import com.ibm.nmon.gui.info.MetadataInfoDialog;
import com.ibm.nmon.gui.info.ProcessInfoDialog;
import com.ibm.nmon.gui.info.SystemInfoDialog;

import com.ibm.nmon.gui.file.CSVFileChooser;
import com.ibm.nmon.util.CSVWriter;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.SystemDataSet;

import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.ProcessDataType;

final class TreeMouseListener extends TreePathParser implements MouseListener {
    protected final NMONVisualizerGui gui;
    protected final JTree tree;

    protected MouseEvent event;

    protected TreeMouseListener(NMONVisualizerGui gui, JTree tree) {
        this.gui = gui;
        this.tree = tree;
    }

    @Override
    public final void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) { // right click only
            // use getClosestRow to allow users to click to the left and right of nodes...
            int row = tree.getClosestRowForLocation(e.getX(), e.getY());

            TreePath selectionPath = null;

            // but check if the click is actually below the tree
            // this will happen on the last node in the tree, in which case the file load menu
            // should popup instead
            if ((row != -1) && (tree.getRowBounds(row).getMaxY() >= e.getY())) {
                selectionPath = tree.getPathForRow(row);
            }

            if (selectionPath != null) {
                // set selected node on right click; not the default behavior
                tree.setSelectionPath(selectionPath);
            }

            event = e;
            parse(selectionPath);
            event = null;
        }
    }

    @Override
    protected final void onNullPath() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem item = new JMenuItem("Load Files..");
        item.addActionListener(new FileLoadAction(gui));
        menu.add(item);

        item = new JMenuItem("Remove All");
        item.addActionListener(new RemoveAllDataSetsAction(gui, gui.getMainFrame()));
        menu.add(item);

        menu.show(event.getComponent(), event.getX(), event.getY());
    }

    @Override
    protected void onRootPath() {
        if (gui.getDataSetCount() > 0) {
            JPopupMenu menu = new JPopupMenu();

            JMenuItem item = new JMenuItem("Save Summary Charts...");
            item.addActionListener(saveChartsAction);
            menu.add(item);

            menu.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    @Override
    protected final void onDataSetPath(DataSet data) {
        buildDataSetMenu(data).show(event.getComponent(), event.getX(), event.getY());
    }

    @Override
    protected void onTypePath(DataSet data, DataType type) {
        buildTypeMenu(data, type, type.getFields()).show(event.getComponent(), event.getX(), event.getY());
    }

    @Override
    protected void onFieldPath(DataSet data, DataType type, String field) {
        buildTypeMenu(data, type, java.util.Collections.singletonList(field)).show(event.getComponent(), event.getX(),
                event.getY());
    }

    private JPopupMenu buildDataSetMenu(final DataSet data) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem item = new JMenuItem("Save to CSV...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CSVFileChooser(gui, data, null, null).saveToCSV();
            }
        });
        menu.add(item);

        item = new JMenuItem("Save Charts...");
        item.addActionListener(saveChartsAction);
        menu.add(item);

        menu.addSeparator();

        if (data.getClass().equals(SystemDataSet.class)) {
            SystemDataSet systemData = (SystemDataSet) data;
            boolean needsSeparator = false;

            if (systemData.getMetadataCount() > 0) {
                item = new JMenuItem("Parsed File Info");

                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new MetadataInfoDialog(gui, (SystemDataSet) data).setVisible(true);
                    }
                });

                menu.add(item);
                needsSeparator = true;
            }

            if (systemData.getSystemInfoCount() > 0) {
                item = new JMenuItem("System Info");

                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new SystemInfoDialog(gui, (SystemDataSet) data).setVisible(true);
                    }
                });

                menu.add(item);
                needsSeparator = true;
            }

            if (needsSeparator) {
                menu.addSeparator();
            }

            item = new JMenuItem("Merge...");

            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new MergeDataSetDialog(gui, (SystemDataSet) data).setVisible(true);
                };
            });

            menu.add(item);
        }

        item = new JMenuItem("Remove");

        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.removeDataSet(data);
            }
        });

        menu.add(item);

        return menu;
    }

    private JPopupMenu buildTypeMenu(final DataSet data, final DataType type, final List<String> fields) {
        JPopupMenu menu = new JPopupMenu();

        if (type.getClass().equals(ProcessDataType.class)) {
            final ProcessDataType processType = (ProcessDataType) type;

            // not aggregated process data
            if (processType.getProcess().getId() != -1) {
                JMenuItem item = new JMenuItem("Process Info");

                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new ProcessInfoDialog(gui, tree, processType.getProcess()).setVisible(true);
                    }
                });

                menu.add(item);
                menu.addSeparator();
            }
        }

        JMenuItem item = new JMenuItem("Copy");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringWriter writer = new StringWriter(4096);
                try {
                    CSVWriter.write(data, type, fields, gui.getIntervalManager().getCurrentInterval(), writer);
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                    return;
                }
                gui.getMainFrame().getToolkit().getSystemClipboard()
                        .setContents(new StringSelection(writer.toString()), null);
            }
        });
        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("Save to CSV...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CSVFileChooser(gui, data, type, fields).saveToCSV();
            }
        });
        menu.add(item);

        item = new JMenuItem("Save Chart...");
        item.addActionListener(saveChartsAction);
        menu.add(item);

        return menu;
    }

    @Override
    public final void mouseClicked(MouseEvent e) {}

    @Override
    public final void mousePressed(MouseEvent e) {
        mouseReleased(e);
    }

    @Override
    public final void mouseEntered(MouseEvent e) {}

    @Override
    public final void mouseExited(MouseEvent e) {}

    private final ActionListener saveChartsAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            gui.getViewManager().saveCharts();
        }
    };
}
