package com.ibm.nmon.gui.main;

import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataSetListener;
import com.ibm.nmon.data.transform.name.HostRenamerFactory;
import com.ibm.nmon.data.transform.name.HostRenamer;
import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.file.FileLoadAction;
import com.ibm.nmon.gui.file.GUIFileChooser;
import com.ibm.nmon.gui.chart.annotate.AnnotationCache;
import com.ibm.nmon.gui.data.RemoveAllDataSetsAction;
import com.ibm.nmon.gui.interval.RemoveAllIntervalsAction;
import com.ibm.nmon.gui.interval.IntervalManagerDialog;
import com.ibm.nmon.gui.util.GranularityDialog;
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
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.reflect.Method;

/**
 * Main menu bar for the application. Listens for interval changes and some property changes so the menu items stay in
 * sync with the rest of the UI.
 */
final class MainMenu extends JMenuBar implements IntervalListener, DataSetListener, PropertyChangeListener {
    private static final long serialVersionUID = -7255908769208090151L;

    private final NMONVisualizerGui gui;

    // 1-based index -- see createHelpMenu()
    private int oracleJVMHeapDumpCount = 1;

    MainMenu(NMONVisualizerGui gui) {
        super();

        assert gui != null;

        this.gui = gui;

        add(createFileMenu());
        add(createViewMenu());
        add(createIntervalsMenu());
        add(createOptionsMenu());
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

        menu.addSeparator();

        item = new JMenuItem("Remove All");
        item.setMnemonic('a');
        item.setIcon(Styles.CLEAR_ICON);
        item.setEnabled(false);
        item.addActionListener(new RemoveAllDataSetsAction(gui, gui.getMainFrame()));

        menu.add(item);

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
        item.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK));

        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getIntervalManager().setCurrentInterval(Interval.DEFAULT);
            }
        });

        AbstractAction doClick = new AbstractAction() {
            private static final long serialVersionUID = -9151414102717456362L;

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

                    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + n,
                            InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "doClick");

                    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + n,
                            InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "doClick");

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
                new GranularityDialog(gui, gui.getMainFrame()).setVisible(true);
            }
        });

        menu.add(item);

        menu.addSeparator();

        JMenu chartSubMenu = new JMenu("Chart");
        chartSubMenu.setMnemonic('c');
        chartSubMenu.setEnabled(false);

        item = new JMenuItem("Table Columns...");
        item.setMnemonic('c');
        item.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

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

        checkItem = new JCheckBoxMenuItem("Line Chart Legends");
        checkItem.setMnemonic('l');
        checkItem.setSelected(gui.getBooleanProperty("lineChartLegend"));
        checkItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));

        checkItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("lineChartLegend", ((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });

        chartSubMenu.add(checkItem);

        chartSubMenu.addSeparator();

        item = new JMenuItem("Clear Annotations");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationCache.clear();
            }
        });

        chartSubMenu.add(item);

        item = new JMenuItem("Remove Last Line");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationCache.removeLastMarker();
            }
        });

        chartSubMenu.add(item);

        item = new JMenuItem("Remove Last Text");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AnnotationCache.removeLastAnnotation();
            }
        });

        chartSubMenu.add(item);

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
                gui.showReportFrame();
            }
        });

        menu.add(item);

        return menu;
    }

    private JMenu createOptionsMenu() {
        JMenu menu = new JMenu("Options");
        menu.setMnemonic('o');

        JMenu namingSubMenu = new JMenu("Name Systems");
        ButtonGroup group = new ButtonGroup();
        String systemsNamedBy = gui.getProperty("systemsNamedBy");

        JCheckBoxMenuItem checkItem = new JCheckBoxMenuItem("By Hostname");
        checkItem.setSelected("host".equals(systemsNamedBy));
        checkItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("systemsNamedBy", "host");
                gui.setHostRenamer(HostRenamer.BY_HOST);
            }
        });

        namingSubMenu.add(checkItem);
        group.add(checkItem);

        checkItem = new JCheckBoxMenuItem("By LPAR Name");
        checkItem.setSelected("lpar".equals(systemsNamedBy));
        checkItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("systemsNamedBy", "lpar");
                gui.setHostRenamer(HostRenamer.BY_LPAR);
            }
        });

        namingSubMenu.add(checkItem);
        group.add(checkItem);

        checkItem = new JCheckBoxMenuItem("By NMON Run Name");
        checkItem.setSelected("run".equals(systemsNamedBy));
        checkItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("systemsNamedBy", "run");
                gui.setHostRenamer(HostRenamer.BY_RUN);
            }
        });

        namingSubMenu.add(checkItem);
        group.add(checkItem);

        checkItem = new JCheckBoxMenuItem("By Custom File...");
        checkItem.setSelected("custom".equals(systemsNamedBy));
        checkItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GUIFileChooser chooser = new GUIFileChooser(gui, "Select Hostname Definitions", "hostnames.json");

                if (chooser.showDialog(gui.getMainFrame(), "Load") == JFileChooser.APPROVE_OPTION) {
                    try {
                        HostRenamer renamer = HostRenamerFactory.loadFromFile(chooser.getSelectedFile());
                        gui.setProperty("systemsNamedBy", "custom");
                        gui.setHostRenamer(renamer);
                    }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(gui.getMainFrame(),
                                "Error parsing file '" + chooser.getSelectedFile().getName() + "'.\n" + ex.getMessage(),
                                "Parse Error", JOptionPane.ERROR_MESSAGE);

                        // reset to 'host'
                        ButtonGroup group = ((javax.swing.DefaultButtonModel) ((JCheckBoxMenuItem) e.getSource())
                                .getModel()).getGroup();
                        group.getElements().nextElement().doClick();
                    }
                }
            }
        });

        namingSubMenu.add(checkItem);
        group.add(checkItem);

        menu.add(namingSubMenu);

        checkItem = new JCheckBoxMenuItem("Scale Process Data by CPUs");
        checkItem.setMnemonic('c');
        checkItem.setSelected(gui.getBooleanProperty("scaleProcessesByCPUs"));

        checkItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("scaleProcessesByCPUs", ((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });

        menu.add(checkItem);

        checkItem = new JCheckBoxMenuItem("Show Status Bar");
        checkItem.setMnemonic('b');
        checkItem.setSelected(gui.getBooleanProperty("showStatusBar"));

        checkItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.setProperty("showStatusBar", ((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });

        menu.add(checkItem);

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
                JOptionPane.showMessageDialog(gui.getMainFrame(), message, "What Now?",
                        JOptionPane.INFORMATION_MESSAGE);
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
                else {
                    gui.getLogViewer().toFront();
                }

                gui.getLogViewer().setLocationRelativeTo(gui.getMainFrame());
            }
        });

        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("Java Info");
        item.setMnemonic('j');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long totalHeapUsage = 0;
                long maxHeapUsage = 0;

                StringBuilder builder = new StringBuilder(1024);

                builder.append(System.getProperty("java.runtime.name"));
                builder.append('\n');
                builder.append("Version ");
                builder.append(": ");
                builder.append(System.getProperty("java.runtime.version"));
                builder.append('\n');
                builder.append("Java Home");
                builder.append(": ");
                builder.append(System.getProperty("java.home"));
                builder.append('\n');
                builder.append('\n');

                builder.append("Classpath");
                builder.append('\n');
                builder.append(System.getProperty("java.class.path").replace(';', '\n'));
                builder.append('\n');
                builder.append('\n');

                builder.append("Memory Pools");
                builder.append('\n');

                for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                    long used = pool.getUsage().getUsed();
                    long max = pool.getUsage().getMax();

                    max = max == -1 ? pool.getUsage().getCommitted() : max;

                    builder.append(pool.getName());
                    builder.append(": ");
                    builder.append(Styles.NUMBER_FORMAT.format(used / 1024.0 / 1024.0));
                    builder.append(" MB");
                    builder.append(" (of ");
                    builder.append(Styles.NUMBER_FORMAT.format(max / 1024.0 / 1024.0));
                    builder.append(" max)");
                    builder.append('\n');

                    totalHeapUsage += used;
                    maxHeapUsage += max;
                }
                builder.append('\n');

                builder.append("Total Heap Usage");
                builder.append('\n');
                builder.append(Styles.NUMBER_FORMAT.format(totalHeapUsage / 1024.0 / 1024.0));
                builder.append(" MB");
                builder.append(" (of ");
                builder.append(Styles.NUMBER_FORMAT.format(maxHeapUsage / 1024.0 / 1024.0));
                builder.append(" max)");

                JOptionPane.showMessageDialog(gui.getMainFrame(), builder.toString(), "Java Info",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menu.add(item);

        item = new JMenuItem("Heap Dump");
        item.setMnemonic('d');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                String vmVendor = System.getProperty("java.vm.vendor");

                if (vmVendor.contains("IBM")) {
                    try {
                        Class<?> ibmDump = Class.forName("com.ibm.jvm.Dump");
                        Method ibmHeapDump = ibmDump.getMethod("HeapDump");
                        ibmHeapDump.invoke(null);
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                gui.getMainFrame(), "Could not complete heap dump on " + "IBM" + " JVM\n\n"
                                        + e.getClass().getName() + ": " + e.getMessage(),
                                "Heap Dump Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else if (vmVendor.contains("Oracle")) {
                    // assume runtime name is processid@hostname
                    String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                    pid = pid.substring(0, pid.indexOf('@'));

                    java.util.Date now = new java.util.Date();
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd.HHmmss.");
                    java.text.DecimalFormat numberFormat = new java.text.DecimalFormat("0000");

                    // format filename like IBM heap dump
                    String filename = "./heapdump." + dateFormat.format(now) + pid + '.'
                            + numberFormat.format(oracleJVMHeapDumpCount++) + ".hprof";

                    try {
                        Class<?> sunVM = Class.forName("sun.tools.attach.HotSpotVirtualMachine");
                        Method sunVMAttach = sunVM.getMethod("attach", String.class);
                        Object attachedVM = sunVMAttach.invoke(null, pid);

                        Class<?> actualVMClass = attachedVM.getClass();
                        Method dumpHeap = actualVMClass.getMethod("dumpHeap", Object[].class);
                        dumpHeap.invoke(attachedVM, new Object[] { new Object[] { filename, "-all" } });

                        Method detach = actualVMClass.getMethod("detach");
                        detach.invoke(attachedVM);
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(gui.getMainFrame(),
                                "Could not complete heap dump on " + "Oracle" + " JVM\n\n" + e.getClass().getName()
                                        + ": " + e.getMessage()
                                        + "\n\nAre you running a JDK?\nIs tools.jar in the classpath?",
                                "Heap Dump Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else {
                    JOptionPane.showMessageDialog(gui.getMainFrame(),
                            "Could not complete heap dump on " + vmVendor + " JVM\n\n", "Heap Dump Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        menu.add(item);

        item = new JMenuItem("Run GC");
        item.setMnemonic('g');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long total = 0;

                for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                    total += pool.getUsage().getUsed();
                }

                double before = total / 1024.0 / 1024.0;

                System.gc();
                System.gc();
                Thread.yield();

                total = 0;

                for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                    total += pool.getUsage().getUsed();
                }

                double after = total / 1024.0 / 1024.0;

                JOptionPane.showMessageDialog(gui.getMainFrame(),
                        "Heap Free Before GC: " + Styles.NUMBER_FORMAT.format(before) + " MB" + '\n'
                                + "Heap Free After GC: " + Styles.NUMBER_FORMAT.format(after) + " MB",
                        "Garbage Collection", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menu.add(item);

        menu.addSeparator();

        item = new JMenuItem("About");
        item.setMnemonic('a');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(gui.getMainFrame(),
                        "Copyright \u00A9 2011-2014\n"
                                + "IBM Software Group, Collaboration Services.\nAll Rights Reserved.\n\n"
                                + "Support is on an 'as-is', 'best-effort' basis only.\n\n" + "Version "
                                + VersionInfo.getVersion() + "\n\n" + "Icons from "
                                + "http://www.famfamfam.com/lab/icons/silk/" + "\n" + "Creative Commons Attribution 2.5"
                                + " License\n(" + "http://creativecommons.org/licenses/by/2.5/legalcode" + ")",
                        "NMON Visualizer", JOptionPane.INFORMATION_MESSAGE);
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
        JMenuItem item = this.getMenu(0).getItem(2);
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
        JMenuItem item = this.getMenu(0).getItem(2);
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
