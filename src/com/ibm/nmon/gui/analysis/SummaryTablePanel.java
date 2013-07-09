package com.ibm.nmon.gui.analysis;

import java.awt.BorderLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

import javax.swing.DefaultComboBoxModel;

import javax.swing.Icon;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.TransferHandler;

import javax.swing.SwingConstants;

import javax.swing.table.TableRowSorter;

import com.ibm.nmon.analysis.AnalysisSet;
import com.ibm.nmon.analysis.AnalysisSetListener;
import com.ibm.nmon.analysis.Statistic;
import com.ibm.nmon.data.DataType;

import com.ibm.nmon.gui.GUITable;
import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.dnd.TableTransferHandler;
import com.ibm.nmon.gui.file.AnalysisSetFileChooser;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.table.ChoosableColumnTableModel;
import com.ibm.nmon.gui.table.DoubleCellRenderer;
import com.ibm.nmon.gui.table.IntegerCellRenderer;
import com.ibm.nmon.gui.table.StringCellRenderer;
import com.ibm.nmon.gui.table.TableColumnChooser;
import com.ibm.nmon.gui.util.ScrollingTableFix;

import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.interval.IntervalListener;

/**
 * Holder panel for summary data. Holds a scrolling JTable which supports drag and drop from the
 * tree of parsed files. The table is actually 2 tables, one that shows summary data (min, max,
 * average, std dev) for selected measurements and another that shows only a single statistic. The
 * latter table is meant for easy entry into a spreadsheet for test result tracking but both tables
 * support copying CSV formatted text to the clipboard.
 */
public final class SummaryTablePanel extends JPanel implements IntervalListener, AnalysisSetListener,
        PropertyChangeListener {
    private static final Icon TRANSPOSE_ICON = Styles.buildIcon("arrow_rotate_clockwise.png");

    private final NMONVisualizerGui gui;

    private final GUITable dataSetTable;
    private final GUITable statisticsTable;

    private final JPanel statsPanel;

    private final JScrollPane scrollPane;
    private final JMenu menu;

    private final AnalysisSet analysisSet = new AnalysisSet();

    private final AnalysisSetFileChooser fileChooser;

    @SuppressWarnings("unchecked")
    public SummaryTablePanel(NMONVisualizerGui gui) {
        super();

        this.gui = gui;

        fileChooser = new AnalysisSetFileChooser(gui, analysisSet);
        menu = new JMenu("Table");
        setupMenu();

        setLayout(new BorderLayout());

        // combo box with various data statistics for use with the results table
        statsPanel = new JPanel();
        setupStatsPanel();

        JPanel top = new JPanel(new BorderLayout());
        setupTopPanel(top);
        add(top, BorderLayout.PAGE_START);

        // each table has its own data model
        dataSetTable = new ByDataSetTable(gui);
        statisticsTable = new GUITable(gui);

        ByDataSetTableModel dataSetTableModel = new ByDataSetTableModel(gui, analysisSet);
        ByStatisticTableModel statTableModel = new ByStatisticTableModel(gui, analysisSet);

        dataSetTable.setModel(dataSetTableModel);
        statisticsTable.setModel(statTableModel);

        setupTable(dataSetTable);
        setupTable(statisticsTable);

        scrollPane = new JScrollPane(statisticsTable);
        scrollPane.getViewport().setBackground(java.awt.Color.WHITE);
        // could be inefficient to have both tables updating when only 1 is visible...
        scrollPane.addComponentListener(new ScrollingTableFix(statisticsTable, scrollPane));
        scrollPane.addComponentListener(new ScrollingTableFix(dataSetTable, scrollPane));
        scrollPane.setBorder(Styles.createBottomLineBorder(this));

        add(scrollPane, BorderLayout.CENTER);

        // alert users they can drag onto the tables
        JLabel label = new JLabel("Click and drag measurements from the tree onto this table");
        label.setFont(Styles.BOLD);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        add(label, BorderLayout.PAGE_END);

        gui.getIntervalManager().addListener(this);
        gui.addPropertyChangeListener("granularity", this);

        analysisSet.addListener(this);

        // check the count column, if it is 0, do not display
        ((TableRowSorter<ByStatisticTableModel>) statisticsTable.getRowSorter())
                .setRowFilter(new RowFilter<ByStatisticTableModel, Integer>() {
                    @Override
                    public boolean include(RowFilter.Entry<? extends ByStatisticTableModel, ? extends Integer> entry) {
                        ByStatisticTableModel model = ((ByStatisticTableModel) entry.getModel());
                        int idx = model.getColumnIndex(Statistic.COUNT.toString());

                        if (idx == -1) {
                            throw new IllegalStateException(model + "has no column named " + Statistic.COUNT.toString());
                        }
                        else {
                            Object value = model.getEnabledValueAt(entry.getIdentifier(), idx);
                            int i = (Integer) value;
                            return i != 0;
                        }
                    }
                });
    }

    private void updateTable() {
        if (statsPanel.isVisible()) {
            ((AnalysisSetTableModel) dataSetTable.getModel()).fireTableDataChanged();
        }
        else {
            ((AnalysisSetTableModel) statisticsTable.getModel()).fireTableDataChanged();
        }
    }

    // when the analysis is not enabled, do not update the table
    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            gui.getIntervalManager().addListener(this);

            // note update setSaveEnabled() if menu position changes
            gui.getMainFrame().getJMenuBar().add(menu, 3);
            gui.getMainFrame().getJMenuBar().revalidate();

            updateTable();
        }
        else {
            gui.getIntervalManager().removeListener(this);

            gui.getMainFrame().getJMenuBar().remove(menu);
            gui.getMainFrame().getJMenuBar().revalidate();
        }

        // note there is no need to remove the 2 table models as DataSetListeners and
        // AnalysisSetListeners
        // the only thing that adds data to the analysis set is drag and drop, which is impossible
        // if this panel is not displayed
        // adding or removing a DataSet fires table changes, but the table is not refreshed until it
        // is displayed

        super.setEnabled(enabled);
    }

    // this class is an IntervalListener rather than each of the table models so that only 1 table
    // model can be updated on interval events
    @Override
    public void intervalAdded(Interval interval) {}

    @Override
    public void intervalRemoved(Interval interval) {}

    @Override
    public void intervalsCleared() {}

    @Override
    public void currentIntervalChanged(Interval interval) {
        updateTable();
    }

    @Override
    public void intervalRenamed(Interval interval) {}

    @Override
    public void analysisAdded(DataType type) {
        setSaveEnabled();
    }

    @Override
    public void analysisAdded(DataType type, String field) {
        setSaveEnabled();
    }

    @Override
    public void analysisRemoved(DataType type) {
        setSaveEnabled();
    }

    @Override
    public void analysisRemoved(DataType type, String field) {
        setSaveEnabled();
    }

    @Override
    public void analysisCleared() {
        setSaveEnabled();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("granularity".equals(evt.getPropertyName())) {
            updateTable();

            String newName = Statistic.GRANULARITY_MAXIMUM.getName(gui.getGranularity());

            @SuppressWarnings("unchecked")
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) ((JComboBox<String>) statsPanel
                    .getComponent(1)).getModel();

            boolean reselect = false;

            if (model.getSelectedItem() instanceof String) {
                reselect = true;
            }

            model.removeElementAt(3);
            model.insertElementAt(newName, 3);

            if (reselect) {
                model.setSelectedItem(newName);
            }
        }
    }

    private void setSaveEnabled() {
        boolean enabled = analysisSet.size() > 0;

        gui.getMainFrame().getJMenuBar().getMenu(3).getItem(1).setEnabled(enabled);
    }

    private void setupTable(final JTable table) {
        table.setDragEnabled(true);
        table.setTransferHandler(new TableTransferHandler(table, analysisSet));

        table.setDefaultRenderer(Double.class, new DoubleCellRenderer());
        table.setDefaultRenderer(Integer.class, new IntegerCellRenderer());
        table.setDefaultRenderer(String.class, new StringCellRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // right click only if any rows are selected
                if (e.isPopupTrigger()) {
                    JPopupMenu menu = new JPopupMenu();

                    if ((table.rowAtPoint(e.getPoint()) != -1) && table.getSelectedRowCount() > 0) {
                        JMenuItem item = new JMenuItem("Copy");

                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                JTable table = statsPanel.isVisible() ? dataSetTable : statisticsTable;

                                table.getTransferHandler().exportToClipboard(table,
                                        SummaryTablePanel.this.gui.getMainFrame().getToolkit().getSystemClipboard(),
                                        TransferHandler.COPY);
                            }
                        });
                        menu.add(item);

                        item = new JMenuItem("Copy All");
                        item.addActionListener(copyTable);
                        menu.add(item);

                        menu.addSeparator();

                        item = new JMenuItem("Remove");

                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Set<String> keys = new java.util.HashSet<String>();

                                for (int row : table.getSelectedRows()) {
                                    keys.add(((AnalysisSetTableModel) table.getModel()).getKey(table
                                            .convertRowIndexToModel(row)));
                                }

                                for (String key : keys) {
                                    analysisSet.removeData(key);
                                }
                            }
                        });

                        menu.add(item);

                        menu.addSeparator();
                    }

                    JMenuItem item = new JMenuItem("Select Columns...");

                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JTable table = statsPanel.isVisible() ? dataSetTable : statisticsTable;
                            ChoosableColumnTableModel model = (ChoosableColumnTableModel) table.getModel();

                            new TableColumnChooser(gui, model);
                        }
                    });

                    menu.add(item);

                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void setupStatsPanel() {
        statsPanel.setVisible(false);

        JLabel label = new JLabel("Statistic:");
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        label.setFont(Styles.LABEL);

        statsPanel.add(label);

        JComboBox<String> statistic = new JComboBox<String>();

        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) statistic.getModel();
        model.addElement(Statistic.AVERAGE.toString());
        model.addElement(Statistic.MINIMUM.toString());
        model.addElement(Statistic.MAXIMUM.toString());
        model.addElement(Statistic.GRANULARITY_MAXIMUM.getName(gui.getGranularity()));
        model.addElement(Statistic.STD_DEV.toString());
        model.addElement(Statistic.MEDIAN.toString());
        model.addElement(Statistic.SUM.toString());
        model.addElement(Statistic.COUNT.toString());

        statistic.setSelectedItem(Statistic.AVERAGE);

        statistic.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                Object o = ((JComboBox<String>) e.getSource()).getModel().getSelectedItem();

                if (o.getClass().equals(String.class)) {
                    ((ByDataSetTableModel) dataSetTable.getModel()).setStatistic(Statistic.GRANULARITY_MAXIMUM);
                }
                else {
                    ((ByDataSetTableModel) dataSetTable.getModel()).setStatistic((Statistic) o);
                }
            }
        });

        statsPanel.add(statistic);
    }

    private void setupTopPanel(JPanel top) {
        JButton copy = new JButton("Copy");
        copy.setIcon(Styles.COPY_ICON);
        copy.addActionListener(copyTable);

        JButton clear = new JButton("Clear");
        clear.setIcon(Styles.CLEAR_ICON);
        clear.addActionListener(clearTable);

        // holder so both buttons are grouped on the edge of the panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(copy);
        buttonPanel.add(clear);

        // switches between the two tables
        JButton transpose = new JButton("Transpose");
        transpose.setIcon(TRANSPOSE_ICON);

        transpose.addActionListener(transposeTable);

        // use temp panel to keep button from filling the space
        JPanel temp = new JPanel();
        temp.add(transpose);

        // check box, stats combo box and buttons all at the top
        top.add(temp, BorderLayout.LINE_START);
        top.add(statsPanel, BorderLayout.CENTER);
        top.add(buttonPanel, BorderLayout.LINE_END);
    }

    private void setupMenu() {
        menu.setMnemonic('t');

        JMenuItem item = new JMenuItem("Load Definition...");
        item.setMnemonic('n');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(loadAnalysis);
        menu.add(item);

        item = new JMenuItem("Save Definition...");
        item.setMnemonic('s');
        item.setIcon(Styles.SAVE_ICON);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(saveAnalysis);
        item.setEnabled(analysisSet.size() > 0);
        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("Copy All");
        item.setMnemonic('a');
        item.setIcon(Styles.COPY_ICON);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK
                | InputEvent.SHIFT_DOWN_MASK));
        item.addActionListener(copyTable);
        menu.add(item);

        item = new JMenuItem("Clear");
        item.setMnemonic('c');
        item.setIcon(Styles.CLEAR_ICON);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK
                | InputEvent.SHIFT_DOWN_MASK));
        item.addActionListener(clearTable);
        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("Select Columns...");
        item.setMnemonic('m');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK
                | InputEvent.SHIFT_DOWN_MASK));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTable table = statsPanel.isVisible() ? dataSetTable : statisticsTable;

                new TableColumnChooser(gui, ((AnalysisSetTableModel) table.getModel()));
            }
        });
        menu.add(item);

        item = new JMenuItem("Transpose");
        item.setMnemonic('t');
        item.setIcon(TRANSPOSE_ICON);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK
                | InputEvent.SHIFT_DOWN_MASK));
        item.addActionListener(transposeTable);
        menu.add(item);
    }

    private final ActionListener copyTable = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JTable table = statsPanel.isVisible() ? dataSetTable : statisticsTable;

            table.selectAll();

            SummaryTablePanel.this.gui.getMainFrame().getToolkit().getSystemClipboard()
                    .setContents(((TableTransferHandler) table.getTransferHandler()).copyAll(), null);
        }
    };

    private final ActionListener clearTable = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            analysisSet.clearData();
        }
    };

    private final ActionListener saveAnalysis = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (analysisSet.size() > 0) {
                fileChooser.save();
            }
        }
    };

    private final ActionListener loadAnalysis = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            fileChooser.load();
        }
    };

    private final Action transposeTable = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (statsPanel.isVisible()) {
                statsPanel.setVisible(false);
                scrollPane.setViewportView(statisticsTable);
                ((AnalysisSetTableModel) statisticsTable.getModel()).fireTableDataChanged();
            }
            else {
                statsPanel.setVisible(true);
                scrollPane.setViewportView(dataSetTable);
                ((AnalysisSetTableModel) dataSetTable.getModel()).fireTableDataChanged();
            }

            SummaryTablePanel.this.requestFocus();
        }
    };
}
