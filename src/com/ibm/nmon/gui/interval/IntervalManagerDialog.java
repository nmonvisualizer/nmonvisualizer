package com.ibm.nmon.gui.interval;

import java.awt.BorderLayout;

import java.awt.KeyboardFocusManager;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.ibm.nmon.gui.GUIDialog;
import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.file.IntervalFileChooser;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.gui.time.SystemTimeList;
import com.ibm.nmon.gui.time.SystemTimeTableModel;

import com.ibm.nmon.gui.util.TimeZoneComboBox;

import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.interval.IntervalListener;
import com.ibm.nmon.interval.IntervalManager;

import com.ibm.nmon.data.DataSet;

/**
 * Dialog to allow adding and removing of Intervals.
 */
public final class IntervalManagerDialog extends GUIDialog implements IntervalListener {
    private static final long serialVersionUID = 4964215301504994528L;

    private final IntervalList intervals;
    private final SystemTimeList systemTimes;

    private final AbsoluteTimeIntervalPanel absolute;
    private final RelativeTimeIntervalPanel relative;
    private final BulkIntervalPanel bulk;

    private final TimeZoneComboBox timeZones;

    private final JButton save;

    private IntervalFileChooser fileChooser;

    public IntervalManagerDialog(NMONVisualizerGui gui) {
        super(gui, gui.getMainFrame(), "Manage Intervals");
        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        setIconImage(Styles.INTERVAL_ICON.getImage());

        timeZones = new TimeZoneComboBox(gui.getDisplayTimeZone());

        timeZones.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IntervalManagerDialog.this.gui.setDisplayTimeZone(timeZones.getSelectedTimeZone());
            }
        });

        JPanel timePanel = new JPanel();

        JLabel label = new JLabel("Time Zone:");
        label.setHorizontalAlignment(SwingConstants.TRAILING);
        label.setFont(Styles.LABEL);

        timePanel.add(label);
        timePanel.add(timeZones);

        AbstractAction loadAction = new AbstractAction() {
            private static final long serialVersionUID = -8590641369327623520L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileChooser == null) {
                    fileChooser = new IntervalFileChooser(IntervalManagerDialog.this.gui);
                }

                fileChooser.load();
            }
        };

        JButton load = new JButton("Load");
        load.addActionListener(loadAction);

        AbstractAction saveAction = new AbstractAction() {
            private static final long serialVersionUID = 8726615183618542317L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileChooser == null) {
                    fileChooser = new IntervalFileChooser(IntervalManagerDialog.this.gui);
                }

                fileChooser.save();
            }
        };

        save = new JButton("Save") {
            private static final long serialVersionUID = 4862470165493725505L;

            public void setEnabled(boolean b) {
                // only associate keyboard shortcut when save is enabled
                super.setEnabled(b);

                if (b) {
                    ((JPanel) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
                }
                else {
                    ((JPanel) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .remove(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
                }
            };
        };
        save.setIcon(Styles.SAVE_ICON);
        save.addActionListener(saveAction);

        JPanel buttons = new JPanel();
        buttons.add(load);
        buttons.add(save);

        JPanel filler = new JPanel();
        filler.setPreferredSize(buttons.getPreferredSize());

        JPanel temp = new JPanel(new BorderLayout());
        temp.add(filler, BorderLayout.LINE_START);
        temp.add(timePanel, BorderLayout.CENTER);
        temp.add(buttons, BorderLayout.LINE_END);

        add(temp);

        absolute = new AbsoluteTimeIntervalPanel(gui);
        relative = new RelativeTimeIntervalPanel(gui);
        bulk = new BulkIntervalPanel(gui);

        JTabbedPane tabs = new JTabbedPane();
        // note change the order here => change the hack in setVisible() for the listeners
        tabs.add("Absolute", absolute);
        tabs.add("Relative", relative);
        tabs.add("Bulk", bulk);

        tabs.setMnemonicAt(0, java.awt.event.KeyEvent.VK_B);
        tabs.setMnemonicAt(1, java.awt.event.KeyEvent.VK_R);
        tabs.setMnemonicAt(2, java.awt.event.KeyEvent.VK_A);

        add(tabs);

        intervals = new IntervalList(gui);
        intervals.setBorder(Styles.DOUBLE_LINE_BORDER);

        add(intervals);

        systemTimes = new SystemTimeList(gui);

        add(systemTimes);

        tabs.addChangeListener(timeSynchronizer);

        systemTimes.getTable().addMouseListener(systemTimesMenu);
        intervals.getTable().addMouseListener(intervalsMenu);

        // clean up internal listeners when this dialog closes
        addHierarchyListener(intervals);
        addHierarchyListener(systemTimes);

        // when an interval is clicked or the start and end times are updated,
        // update the system times
        intervals.addPropertyChangeListener("interval", systemTimes);

        // also update system times when the current data is changed
        absolute.addPropertyChangeListener("interval", systemTimes);
        relative.addPropertyChangeListener("interval", systemTimes);
        bulk.addPropertyChangeListener("interval", systemTimes);

        // keep the system times in sync with the current start and end times
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusManager);

        JPanel content = (JPanel) getContentPane();

        ActionMap actions = content.getActionMap();
        InputMap inputs = content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        actions.put("load", loadAction);
        actions.put("save", saveAction);

        inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "load");
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);

        // ensure the start field for absolute requests focus
        if (true) {
            // hack to toggle state so the first interval panel actually has its listeners set
            absolute.setEnabled(false);
            absolute.setEnabled(true);
            relative.setEnabled(false);
            bulk.setEnabled(false);

            save.setEnabled(gui.getIntervalManager().getIntervalCount() != 0);

            gui.getIntervalManager().addListener(this);
        }
        // do not handle false; assume dialog is only opened once not opened and closed
    }

    @Override
    public void dispose() {
        super.dispose();

        gui.getIntervalManager().removeListener(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusManager);

        // clean up listeners
        absolute.setEnabled(false);
        relative.setEnabled(false);
        bulk.setEnabled(false);
    }

    @Override
    public void intervalAdded(Interval interval) {
        save.setEnabled(true);
    }

    @Override
    public void intervalRemoved(Interval interval) {
        save.setEnabled(gui.getIntervalManager().getIntervalCount() != 0);
    }

    @Override
    public void intervalsCleared() {
        save.setEnabled(false);
    }

    @Override
    public void currentIntervalChanged(Interval interval) {
        intervals.repaint();
    }

    @Override
    public void intervalRenamed(Interval interval) {
        intervals.repaint();
    }

    // display a menu on the system times table that allows the user to set the current interval or
    // start and end times
    private final MouseAdapter intervalsMenu = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            mouseReleased(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            int row = intervals.getTable().rowAtPoint(e.getPoint());

            if (e.isPopupTrigger() && (row >= 0)) {
                intervals.getTable().changeSelection(row, 0, false, false);

                final Interval interval = ((IntervalTableModel) intervals.getTable().getModel()).getValueAt(row);

                JPopupMenu menu = new JPopupMenu();
                JMenuItem item = null;

                item = new JMenuItem("Use as Start & End");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        BaseIntervalPanel active = null;

                        if (absolute.isEnabled()) {
                            active = absolute;
                        }
                        else if (relative.isEnabled()) {
                            active = relative;
                        }
                        else if (bulk.isEnabled()) {
                            active = bulk;
                        }

                        if (active != null) {
                            active.setTimes(interval.getStart(), interval.getEnd());
                            active.revalidate();
                        }
                    }
                });

                menu.add(item);
                if (!interval.equals(gui.getIntervalManager().getCurrentInterval())) {
                    item = new JMenuItem("Set as Current");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            gui.getIntervalManager().setCurrentInterval(interval);
                            intervals.getTable().repaint();
                        }
                    });

                    menu.add(item);
                }

                menu.addSeparator();

                item = new JMenuItem("Rename");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        intervals.getTable().editCellAt(intervals.getTable().getSelectedRow(), 0);
                    }
                });
                menu.add(item);

                menu.addSeparator();

                item = new JMenuItem("Remove");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gui.getIntervalManager().removeInterval(interval);
                        intervals.getTable().repaint();
                    }
                });
                menu.add(item);

                menu.show(e.getComponent(), e.getX(), e.getY());

                // clear the selected system just like when the system times table loses focus
                // this cannot be done in the focus listener since the interval table does not have
                // to gain focus for the menu to show up
                systemTimes.getTable().clearSelection();
            }
        }
    };

    // display a menu on the system times table that allows the user to set intervals or start and
    // end times
    private final MouseAdapter systemTimesMenu = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            mouseReleased(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            int row = systemTimes.getTable().rowAtPoint(e.getPoint());

            if (e.isPopupTrigger() && (row >= 0)) {
                systemTimes.getTable().changeSelection(row, 0, false, false);

                final DataSet data = ((SystemTimeTableModel) systemTimes.getTable().getModel()).getValueAt(row);

                JPopupMenu menu = new JPopupMenu();
                JMenuItem item = new JMenuItem("Use as Start & End");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        BaseIntervalPanel active = null;

                        if (absolute.isEnabled()) {
                            active = absolute;
                        }
                        else if (relative.isEnabled()) {
                            active = relative;
                        }
                        else if (bulk.isEnabled()) {
                            active = bulk;
                        }

                        if (active != null) {
                            active.setTimes(data.getStartTime(), data.getEndTime());
                            active.revalidate();
                        }
                    }
                });

                menu.add(item);

                item = new JMenuItem("Add Interval");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        long startTime = data.getStartTime();
                        long endTime = data.getEndTime();

                        NMONVisualizerGui gui = IntervalManagerDialog.this.gui;

                        // only add if not the default interval
                        if ((startTime != gui.getMinSystemTime()) || (endTime != gui.getMaxSystemTime())) {
                            IntervalManager intervalManager = gui.getIntervalManager();
                            // this interval should always be valid assuming the file times
                            // are valid
                            Interval i = new Interval(startTime, endTime);

                            if (intervalManager.addInterval(i)) {
                                firePropertyChange("interval", intervalManager.getCurrentInterval(), i);

                                intervalManager.setCurrentInterval(i);
                            }

                            gui.getIntervalManager().addInterval(new Interval(startTime, endTime));
                        }
                    }
                });

                menu.add(item);

                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    };

    // when switching between absolute, relative and bulk, update the values so they stay in sync
    private final ChangeListener timeSynchronizer = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            BaseIntervalPanel previous = null;

            if (absolute.isEnabled()) {
                previous = absolute;
            }
            else if (relative.isEnabled()) {
                previous = relative;
            }
            else if (bulk.isEnabled()) {
                previous = bulk;
            }
            else {
                return;
            }

            JTabbedPane tabs = (JTabbedPane) e.getSource();
            BaseIntervalPanel current = (BaseIntervalPanel) tabs.getSelectedComponent();

            if (current != null) {
                current.setIntervalName(previous.getIntervalName());

                current.setTimes(previous.getStartTime(), previous.getEndTime());

                current.setEnabled(true);
            }

            // remove listeners
            previous.setEnabled(false);

            current.propertyChange(new PropertyChangeEvent(this, "timeZone", null, previous.getTimeZone()));
        }
    };

    // when focus returns to the time panels,
    // set the system times interval to the start and end times
    private final PropertyChangeListener focusManager = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue() != null) {
                // absolute parent => tabs
                // if any of the data entry forms are selected, unselect the interval table
                if (SwingUtilities.isDescendingFrom((java.awt.Component) evt.getNewValue(), absolute.getParent())) {
                    intervals.getTable().clearSelection();
                    systemTimes.getTable().clearSelection();
                }
                else if (intervals.getTable() == evt.getNewValue()) {
                    systemTimes.getTable().clearSelection();
                }
            }
        }
    };
}
