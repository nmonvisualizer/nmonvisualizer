package com.ibm.nmon.gui.main;

import javax.swing.ButtonGroup;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataSetListener;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.file.FileLoadAction;

import com.ibm.nmon.gui.data.RemoveAllDataSetsAction;
import com.ibm.nmon.gui.interval.RemoveAllIntervalsAction;

import com.ibm.nmon.gui.interval.IntervalManagerDialog;

import com.ibm.nmon.gui.report.ReportFrame;
import com.ibm.nmon.gui.util.LogViewerDialog;

import com.ibm.nmon.interval.IntervalListener;
import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.util.TimeFormatCache;
import com.ibm.nmon.util.VersionInfo;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Main menu bar for the application. Listens for interval changes and some property changes so the
 * menu items stay in sync with the rest of the UI.
 */
final class MainMenu extends JMenuBar implements IntervalListener, DataSetListener, PropertyChangeListener {
    private final NMONVisualizerGui gui;

    MainMenu(NMONVisualizerGui gui) {
        super();

        assert gui != null;

        this.gui = gui;

        add(createFileMenu());
        add(createViewMenu());
        add(createIntervalsMenu());
        add(createHelpMenu());

        gui.getIntervalManager().addListener(this);
        gui.addDataSetListener(this);
        gui.addPropertyChangeListener("chartsDisplayed", this);
        gui.addPropertyChangeListener("timeZone", this);
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.setMnemonic('f');

        JMenuItem item = new JMenuItem("Load...");
        item.setMnemonic('l');
        item.addActionListener(new FileLoadAction(gui));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        menu.add(item);

        item = new JMenuItem("Remove All");
        item.setMnemonic('a');
        item.setIcon(Styles.CLEAR_ICON);
        item.setEnabled(false);
        item.addActionListener(new RemoveAllDataSetsAction(gui, gui.getMainFrame()));

        menu.add(item);

        menu.addSeparator();

        JCheckBoxMenuItem checkItem = new JCheckBoxMenuItem("Use AIX LPAR Name");
        checkItem.setSelected(gui.getBooleanProperty("usePartitionName"));

        checkItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("usePartitionName", ((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });

        menu.add(checkItem);

        menu.addSeparator();

        item = new JMenuItem("Exit");
        item.setMnemonic('x');

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.exit();
            }
        });

        menu.add(item);

        return menu;
    }

    private JMenu createIntervalsMenu() {
        JMenu menu = new JMenu("Intervals");
        menu.setMnemonic('i');

        ButtonGroup group = new ButtonGroup();

        JMenuItem item = new IntervalMenuItem(Interval.DEFAULT);
        item.setText(TimeFormatCache.formatInterval(Interval.DEFAULT));
        item.setMnemonic('a');
        item.setSelected(true);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK));

        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getIntervalManager().setCurrentInterval(Interval.DEFAULT);
            }
        });

        AbstractAction doClick = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((JMenuItem) e.getSource()).doClick();
            }
        };

        item.getActionMap().put("doClick", doClick);
        InputMap inputMap = item.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
                "doClick");

        group.add(item);
        menu.add(item);

        menu.addSeparator();

        if (gui.getIntervalManager().getIntervalCount() != 0) {
            int n = 1;

            for (Interval interval : gui.getIntervalManager().getIntervals()) {
                item = new IntervalMenuItem(interval);

                if (n < 10) {
                    item.getActionMap().put("doClick", doClick);
                    inputMap = item.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

                    inputMap.put(
                            KeyStroke.getKeyStroke(KeyEvent.VK_0 + n, InputEvent.CTRL_DOWN_MASK
                                    | InputEvent.ALT_DOWN_MASK), "doClick");

                    inputMap.put(
                            KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + n, InputEvent.CTRL_DOWN_MASK
                                    | InputEvent.ALT_DOWN_MASK), "doClick");

                    ++n;
                }

                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gui.getIntervalManager().setCurrentInterval(((IntervalMenuItem) e.getSource()).getInterval());
                    }
                });

                group.add(item);

                if (interval.equals(gui.getIntervalManager().getCurrentInterval())) {
                    item.setSelected(true);
                }

                menu.add(item);
            }

            menu.addSeparator();

            item = new JMenuItem("Remove All");
            item.setMnemonic('r');
            item.setIcon(Styles.CLEAR_ICON);
            item.addActionListener(new RemoveAllIntervalsAction(gui, gui.getMainFrame()));
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK));

            menu.add(item);
        }
        else {
            item = new JMenuItem("<No Intervals>");
            item.setEnabled(false);
            menu.add(item);
        }

        menu.addSeparator();

        item = new JMenuItem("Manage...");
        item.setMnemonic('m');
        item.setIcon(Styles.INTERVAL_ICON);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new IntervalManagerDialog(gui).setVisible(true);
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));

        menu.add(item);

        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic('v');

        JMenuItem item = new JMenuItem("Set Granularity...");
        item.setMnemonic('g');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));

        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new GranularityDialog(gui).setVisible(true);
            }
        });

        menu.add(item);

        menu.addSeparator();

        JMenu chartSubMenu = new JMenu("Chart");
        chartSubMenu.setMnemonic('c');
        chartSubMenu.setEnabled(false);

        item = new JMenuItem("Table Columns...");
        item.setMnemonic('c');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK
                | InputEvent.SHIFT_DOWN_MASK));

        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getViewManager().displayTableColumnChooser();
            }
        });

        chartSubMenu.add(item);

        JCheckBoxMenuItem checkItem = new JCheckBoxMenuItem("Relative Time");
        checkItem.setMnemonic('r');
        checkItem.setSelected(gui.getBooleanProperty("chartRelativeTime"));

        checkItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("chartRelativeTime", ((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });

        chartSubMenu.add(checkItem);

        menu.add(chartSubMenu);

        menu.addSeparator();

        checkItem = new JCheckBoxMenuItem("Summary Table");
        checkItem.setMnemonic('t');
        checkItem.setIcon(Styles.buildIcon("table.png"));
        checkItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        checkItem.setSelected(!gui.getBooleanProperty("chartsDisplayed"));

        checkItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("chartsDisplayed", !((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });

        menu.add(checkItem);

        item = new JMenuItem("Custom Report...");
        item.setMnemonic('r');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        item.setIcon(Styles.REPORT_ICON);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO single ReportFrame per GUI
                new ReportFrame(gui).setVisible(true);
            }
        });

        menu.add(item);

        return menu;
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic('h');

        JMenuItem item = new JMenuItem("What Now?");
        item.setIcon(Styles.buildIcon("help.png"));
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = null;

                if (gui.getDataSetCount() == 0) {
                    message = "Parse some files by:\n\n" + "1) Selecting 'Load' from the File menu,\n"
                            + "2) Right clicking on the left hand side\n    of the application\n"
                            + "3) Dragging files from the filesystem\n    onto the left hand side of the application\n"
                            + "4) CTRL-O";
                }
                else if (gui.getIntervalManager().getIntervalCount() == 0) {
                    message = "Double click on the list of parsed systems to expand them.\n\n"
                            + "Selecting a system will display a summary report.\n"
                            + "Selecting a metric will graph those values.\n"
                            + "Right clicking on the system name will bring up system information.\n\n"
                            + "Add intervals by going to the Intervals menu and selecting Manage Intervals (or CTRL-I)";
                }
                else {
                    message = "Select the interval you want to analyze.\n\n"
                            + "View charts for that interval by clicking on the system tree.\n"
                            + "View summary information for that interval in the summary table (see the 'View' menu).\n"
                            + "\nRight clicking will bring up context sensitive menus";
                }
                JOptionPane.showMessageDialog(gui.getMainFrame(), message, "What Now?", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("View Log...");
        item.setMnemonic('l');
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        item.setIcon(LogViewerDialog.LOG_ICON);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!gui.getLogViewer().isVisible()) {
                    gui.getLogViewer().setVisible(true);
                }
            }
        });

        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("Java Info");
        item.setMnemonic('j');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(
                        gui.getMainFrame(),
                        System.getProperty("java.runtime.name") + '\n' + "Version: "
                                + System.getProperty("java.runtime.version") + '\n' + "Java Home: "
                                + System.getProperty("java.home") + '\n' + '\n' + "Classpath" + '\n'
                                + System.getProperty("java.class.path").replace(';', '\n') + '\n' + '\n'
                                + "Current Heap Usage: "
                                + Styles.NUMBER_FORMAT.format(Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0)
                                + " MB" + " (of "
                                + Styles.NUMBER_FORMAT.format(Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0)
                                + " MB" + " total)", "Java Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menu.add(item);

        item = new JMenuItem("Heap Dump");
        item.setMnemonic('d');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                com.ibm.jvm.Dump.HeapDump();
            }
        });

        menu.add(item);

        item = new JMenuItem("Run GC");
        item.setMnemonic('g');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                double before = Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0;
                System.gc();
                System.gc();
                Thread.yield();
                double after = Runtime.getRuntime().totalMemory() / 1024.0 / 1024.0;
                JOptionPane.showMessageDialog(gui.getMainFrame(),
                        "Heap Used Before GC: " + Styles.NUMBER_FORMAT.format(before) + " MB" + '\n'
                                + "Heap Used After GC: " + Styles.NUMBER_FORMAT.format(after) + " MB",
                        "Garbage Collection", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("About");
        item.setMnemonic('a');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(
                        gui.getMainFrame(),
                        "Copyright \u00A9 2011-2013\n"
                                + "IBM Software Group, Collaboration Services.\nAll Rights Reserved.\n\n"
                                + "This program is for IBM internal use only.\n"
                                + "Support is on an 'as-is', 'best-effort' basis only.\n\n" + "Version "
                                + VersionInfo.getVersion() + "\n\n" + "Icons from "
                                + "http://www.famfamfam.com/lab/icons/silk/" + "\n"
                                + "Creative Commons Attribution 3.0" + " License\n("
                                + "http://creativecommons.org/licenses/by/3.0/" + ")", "NMON Visualizer",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menu.add(item);

        return menu;
    }

    // keep interval menu in sync with the interval manager
    // rather than track what changed or attempt to order the intervals manually, just remove and
    // recreate the menu
    // assume interval menu is second
    @Override
    public void intervalsCleared() {
        remove(2);
        add(createIntervalsMenu(), 2);
    }

    @Override
    public void intervalRemoved(Interval interval) {
        remove(2);
        add(createIntervalsMenu(), 2);
    }

    @Override
    public void intervalAdded(Interval interval) {
        remove(2);
        add(createIntervalsMenu(), 2);
    }

    @Override
    public void currentIntervalChanged(Interval interval) {
        JMenu intervals = getMenu(2);

        for (int i = 0; i < intervals.getItemCount(); i++) {
            JMenuItem item = intervals.getItem(i);

            if ((item != null) && (item.getClass() == IntervalMenuItem.class)) {
                IntervalMenuItem intervalItem = (IntervalMenuItem) item;

                if (intervalItem.getInterval().equals(interval)) {
                    intervalItem.setSelected(true);
                    break;
                }
            }
        }
    }

    public void intervalRenamed(Interval interval) {
        JMenu intervals = getMenu(2);

        for (int i = 0; i < intervals.getItemCount(); i++) {
            JMenuItem item = intervals.getItem(i);

            if ((item != null) && (item.getClass() == IntervalMenuItem.class)) {
                IntervalMenuItem intervalItem = (IntervalMenuItem) item;

                if (intervalItem.getInterval().equals(interval)) {
                    intervalItem.setText(TimeFormatCache.formatInterval(interval));
                    break;
                }
            }
        }
    }

    public void dataAdded(DataSet data) {
        // File -> Remove All
        JMenuItem item = this.getMenu(0).getItem(1);
        item.setEnabled(true);

        changeDefaultIntervalName();
    }

    public void dataRemoved(DataSet data) {
        changeDefaultIntervalName();
    }

    public void dataChanged(DataSet data) {
        changeDefaultIntervalName();
    }

    public void dataCleared() {
        // File -> Remove All
        JMenuItem item = this.getMenu(0).getItem(1);
        item.setEnabled(false);

        changeDefaultIntervalName();
        enableChartSubMenu(false);
    }

    private void changeDefaultIntervalName() {
        // Intervals -> All Data
        JMenuItem item = this.getMenu(2).getItem(0);
        item.setText(TimeFormatCache.formatInterval(Interval.DEFAULT));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("chartsDisplayed".equals(evt.getPropertyName())) {
            boolean chartsDisplayed = (Boolean) evt.getNewValue();

            enableChartSubMenu(chartsDisplayed);

            // View -> Summary Table
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) getMenu(1).getItem(4);
            item.setSelected(!chartsDisplayed);
        }
        else if ("timeZone".equals(evt.getPropertyName())) {
            for (Interval interval : gui.getIntervalManager().getIntervals()) {
                intervalRenamed(interval);
            }
        }
    }

    // default access to allow ViewManager access
    void enableChartSubMenu(boolean enabled) {
        // View -> Chart
        JMenuItem item = getMenu(1).getItem(2);

        if (item.isEnabled() != enabled) {
            item.setEnabled(enabled);
        }
    }

    private final class IntervalMenuItem extends JCheckBoxMenuItem {
        private static final long serialVersionUID = -7947301490892979513L;

        private final Interval interval;

        public IntervalMenuItem(Interval interval) {
            super(TimeFormatCache.formatInterval(interval));
            this.interval = interval;
        }

        public Interval getInterval() {
            return interval;
        }
    }
}
