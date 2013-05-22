package com.ibm.nmon.gui.main;

import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import java.awt.Toolkit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

import com.ibm.nmon.NMONVisualizerApp;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.interval.IntervalPicker;
import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.parser.IOStatParser;
import com.ibm.nmon.util.GranularityHelper;
import com.ibm.nmon.util.TimeFormatCache;

import com.ibm.nmon.gui.tree.TreePanel;
import com.ibm.nmon.gui.util.LogViewerDialog;

import com.ibm.nmon.gui.parse.IOStatPostParser;
import com.ibm.nmon.gui.parse.VerboseGCPreParser;

import com.ibm.nmon.gui.Styles;

public final class NMONVisualizerGui extends NMONVisualizerApp {
    public static void main(String[] args) throws Exception {
        javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    new NMONVisualizerGui().getMainFrame().setVisible(true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static final String DEFAULT_WINDOW_TITLE = "NMON Visualizer";

    private final Preferences preferences;

    private final JFrame mainFrame;

    private final MainMenu menu;

    private final LogViewerDialog logViewer;

    private VerboseGCPreParser gcPreParser;
    private IOStatPostParser ioStatPostParser;

    private final GranularityHelper granularityHelper;

    public NMONVisualizerGui() throws Exception {
        super();

        granularityHelper = new GranularityHelper(this);
        setGranularity(-1); // automatic

        preferences = Preferences.userNodeForPackage(NMONVisualizerGui.class);

        setProperty("chartsDisplayed", true);

        mainFrame = new JFrame(DEFAULT_WINDOW_TITLE);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(windowManager);
        mainFrame.setIconImage(Styles.IBM_ICON.getImage());

        menu = new MainMenu(this);
        mainFrame.setJMenuBar(menu);

        logViewer = new LogViewerDialog(this);

        // tree of parsed files on the left, content on the left
        JSplitPane lrSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        // never resize tree automatically
        lrSplitPane.setResizeWeight(0);

        mainFrame.setContentPane(lrSplitPane);

        // tree of parsed files on the left, content on the right
        TreePanel treePanel = new TreePanel(this);
        JPanel right = new JPanel(new BorderLayout());

        lrSplitPane.setLeftComponent(treePanel);
        lrSplitPane.setRightComponent(right);

        ChartTableToggle toggle = new ChartTableToggle(this);

        JPanel top = new JPanel(new BorderLayout());
        top.add(new IntervalPicker(this), BorderLayout.CENTER);
        top.add(toggle, BorderLayout.LINE_END);

        right.add(top, BorderLayout.PAGE_START);

        // ViewManager's SummaryView adds the checkbox to the top panel
        // so top must already be created and added to the gui before the data panel is initialized
        ViewManager dataPanel = new ViewManager(this);
        treePanel.addTreeSelectionListener(dataPanel);

        right.add(dataPanel, BorderLayout.CENTER);

        mainFrame.pack();
        positionMainFrame();
    }

    /**
     * Gets the Preferences for this application.
     * 
     * @return the Preferences, which will never be <code>null</code>
     */
    public final Preferences getPreferences() {
        return preferences;
    }

    /**
     * Gets the main JFrame used by this class. This frame should be considered the "main window"
     * for the application.
     * 
     * @return the application's main window
     */
    public JFrame getMainFrame() {
        return mainFrame;
    }

    public ViewManager getViewManager() {
        return (ViewManager) ((JPanel) ((JSplitPane) mainFrame.getContentPane()).getRightComponent()).getComponent(1);
    }

    public LogViewerDialog getLogViewer() {
        return logViewer;
    }

    public int getGranularity() {
        return granularityHelper.getGranularity();
    }

    /**
     * Defines how granular charts will be, i.e. how many seconds will pass between data points.
     * This method causes either a <code>automaticGranularity</code> or <code>granularity</code>
     * property change event to be fired.
     * 
     * @param granularity the new granularity, in seconds. A zero or negative value implies that
     *            granularity will be automatically calculated based on the current interval.
     */
    public void setGranularity(int granularity) {
        int oldGranularity = getGranularity();

        if (granularity <= 0) {
            if (getBooleanProperty("automaticGranularity")) {
                granularityHelper.recalculate();
            }
            else {
                granularityHelper.setAutomatic(true);
                setProperty("automaticGranularity", true);
            }
        }
        else {
            granularityHelper.setGranularity(granularity);

            if (getBooleanProperty("automaticGranularity")) {
                setProperty("automaticGranularity", false);
            }
        }

        if (getGranularity() != oldGranularity) {
            setProperty("granularity", getGranularity());
        }
    }

    @Override
    public void currentIntervalChanged(Interval interval) {
        super.currentIntervalChanged(interval);

        if (getBooleanProperty("automaticGranularity")) {
            setGranularity(-1);
        }

        updateWindowTitle(interval);
    }

    @Override
    public void intervalRenamed(Interval interval) {
        super.intervalRenamed(interval);

        if (getIntervalManager().getCurrentInterval().equals(interval)) {
            updateWindowTitle(interval);
        }
    }

    private void updateWindowTitle(Interval interval) {
        if ((getMinSystemTime() > 0) && (getMaxSystemTime() < Long.MAX_VALUE)) {
            mainFrame.setTitle(DEFAULT_WINDOW_TITLE + " - " + TimeFormatCache.formatInterval(interval));
        }
        else {
            mainFrame.setTitle(DEFAULT_WINDOW_TITLE);
        }
    }

    /**
     * Gracefully exits the application. This method asks the user for confirmation through a
     * JOptionPane before continuing. If the user selects "Yes", the application saves all
     * preferences and calls dispose() on the main frame.
     */
    void exit() {
        int confirm = JOptionPane.showConfirmDialog(mainFrame, "Are you sure you want to Exit?", "Exit?",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == 0) {
            // save the window sizes to the Preferences
            // if the window is maximized, do not save the sizes -- keep
            // the old ones so the user can un-maximize later
            if ((mainFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) {
                getPreferences().putBoolean("WindowMaximized", true);
            }
            else {
                getPreferences().putBoolean("WindowMaximized", false);

                getPreferences().putInt("WindowPosX", mainFrame.getX());
                getPreferences().putInt("WindowPosY", mainFrame.getY());

                getPreferences().putInt("WindowSizeX", (int) mainFrame.getSize().getWidth());
                getPreferences().putInt("WindowSizeY", (int) mainFrame.getSize().getHeight());
            }

            logViewer.dispose();
            mainFrame.dispose();

            try {
                preferences.sync();
            }
            catch (java.util.prefs.BackingStoreException e) {
                logger.warn("could not save preferences", e);
            }
        }
    }

    @Override
    protected String[] getDataForGCParse(final String fileToParse) {
        if (gcPreParser == null) {
            gcPreParser = new VerboseGCPreParser(this);
        }

        try {
            // wait here so parsing does not continue in the background, possibly throwing up more
            // preparser dialogs
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    java.io.File file = new java.io.File(fileToParse);
                    java.io.File parent = file.getParentFile();

                    if (parent != null) {
                        gcPreParser.setJVMName(parent.getName());
                    }

                    parent = parent.getParentFile();

                    if (parent != null) {
                        gcPreParser.setHostname(parent.getName());
                    }

                    gcPreParser.preParseDataSet(fileToParse);
                }
            });
        }
        catch (Exception e) {
            logger.error("cannot get hostname and JVM name for file '{}'", fileToParse, e);
        }

        if (gcPreParser.isSkipped()) {
            return null;
        }
        else {
            return new String[] { gcPreParser.getHostname(), gcPreParser.getJVMName() };
        }
    }

    @Override
    protected Object[] getDataForIOStatParse(final String fileToParse, final String hostname) {
        if (ioStatPostParser == null) {
            ioStatPostParser = new IOStatPostParser(this);
        }

        try {
            // wait here so parsing does not continue in the background, possibly throwing up more
            // postparser dialogs
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    if (!hostname.equals(IOStatParser.DEFAULT_HOSTNAME)) {
                        ioStatPostParser.setHostname(hostname);
                    }

                    ioStatPostParser.postParseDataSet(fileToParse);
                }
            });
        }
        catch (Exception e) {
            logger.error("cannot get hostname and JVM name for file '{}'", fileToParse, e);
        }

        if (ioStatPostParser.isSkipped()) {
            return null;
        }
        else {
            return new Object[] { ioStatPostParser.getHostname(), ioStatPostParser.getDate() };
        }
    }

    private void positionMainFrame() {
        // load the window position and sizes from the Preferences
        // use max value here so that first time users / empty prefs create a default sized window
        // centered on the primary display
        int x = getPreferences().getInt("WindowPosX", 0);
        int y = this.getPreferences().getInt("WindowPosY", 0);

        int xSize = getPreferences().getInt("WindowSizeX", Integer.MAX_VALUE);
        int ySize = this.getPreferences().getInt("WindowSizeY", Integer.MAX_VALUE);

        // check to see if the preferred window will be visible with the current screen config
        // (user could have removed / reconfigured multiple monitors or resolutions)
        boolean willBeVisible = false;
        java.awt.Rectangle preferred = new java.awt.Rectangle(x, y, xSize, ySize);

        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        outer: for (GraphicsDevice device : devices) {
            GraphicsConfiguration[] configs = device.getConfigurations();

            for (GraphicsConfiguration config : configs) {
                if (SwingUtilities.isRectangleContainingRectangle(config.getBounds(), preferred)) {
                    willBeVisible = true;
                    break outer;
                }
            }
        }

        if (!willBeVisible) {
            java.awt.Dimension currentScreenSize = Toolkit.getDefaultToolkit().getScreenSize();

            // resize if too big
            if (xSize > currentScreenSize.width) {
                xSize = 800;
            }

            if (ySize > currentScreenSize.height) {
                ySize = 600;
            }

            // center the window on the screen
            x = (currentScreenSize.width / 2) - (xSize / 2);
            y = (currentScreenSize.height / 2) - (ySize / 2);
        }

        mainFrame.setLocation(new java.awt.Point(x, y));
        // set the size even if the window will not be maximized to ensure that
        // the window will be the right size if the user does un-maximize it
        mainFrame.setSize(xSize, ySize);

        if (getPreferences().getBoolean("WindowMaximized", false)) {
            mainFrame.setExtendedState(mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
    }

    @Override
    protected void fireDataAdded(final DataSet data) {
        // fire in the Swing event dispatcher thread if not already running there
        if (SwingUtilities.isEventDispatchThread()) {
            super.fireDataAdded(data);

            if (getBooleanProperty("automaticGranularity")) {
                setGranularity(-1);
            }
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    NMONVisualizerGui.super.fireDataAdded(data);

                    if (getBooleanProperty("automaticGranularity")) {
                        setGranularity(-1);
                    }
                }
            });
        }
    }

    private WindowAdapter windowManager = new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e) {
            // can only update the divider location when the window is visible
            // tree gets 80%
            JSplitPane lrSplit = ((JSplitPane) mainFrame.getContentPane());

            lrSplit.setDividerLocation(0.2);
        };

        @Override
        public void windowClosing(WindowEvent e) {
            NMONVisualizerGui.this.exit();
        }
    };
}
