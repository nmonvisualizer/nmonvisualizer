package com.ibm.nmon.gui.chart;

import org.slf4j.Logger;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.MouseEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import javax.swing.MenuElement;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartTransferable;
import org.jfree.chart.ChartUtilities;

import org.jfree.chart.JFreeChart;

import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;

import org.jfree.ui.ExtensionFileFilter;

import com.ibm.nmon.gui.chart.data.DataTupleDataset;
import com.ibm.nmon.gui.chart.data.DataTupleXYDataset;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.util.CSVWriter;

public class BaseChartPanel extends ChartPanel implements PropertyChangeListener {
    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    protected final NMONVisualizerGui gui;

    protected BaseChartPanel(NMONVisualizerGui gui) {
        // With multiple charts for each ReportPanel and one report panel per DataSet, there could
        // be a large number of active charts. To keep memory usage down, disable using a buffered
        // image despite any performance benefit.
        // Unfortunately, chartBuffer in ChartPanel is private so there is no way to clear the
        // memory used when the chart is not being displayed.
        super(null, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
                DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, false, true, true, false, false, true);

        setBackground(java.awt.Color.WHITE);

        // keep the chart for resizing when empty
        setPreferredSize(null);

        // disable zooming
        removeMouseMotionListener(this);

        // no need for mouse listener when there is no chart
        removeMouseListener(this);

        this.gui = gui;

        setEnabled(false);
        clearChart();
    }

    public final void setChart(JFreeChart chart) {
        JFreeChart old = getChart();

        if (chart != old) {
            super.setChart(chart);

            if (chart != null) {
                // keep in sync with clearChart(); only add a single mouse listener
                if (old == null) {
                    addMouseListener(this);
                }

                firePropertyChange("chart", null, getDataset());
            }
            else {
                // fix memory leak; rendering info holds ChartEntitiy objects that reference the
                // chart's Dataset
                getChartRenderingInfo().clear();
                firePropertyChange("chart", null, null);
            }
        }
    }

    public void clearChart() {
        if (getChart() != null) {
            setChart(null);

            // do not display the popup menu when there is no chart
            removeMouseListener(this);
        }
    }

    /**
     * <p>
     * Visually indicate on the that the element (bar or line) corresponding to the
     * <code>Dataset's</code> given row and column.
     * </p>
     * 
     * <p>
     * This method <em>should not</em> fire a property change event since this is meant to be called
     * in response to other user input. This function should toggle already highlighted elements to
     * an unhighlighted state.
     * </p>
     * 
     * @param row the chart's dataset row
     * @param column the chart's dataset column
     */
    public void highlightElement(int row, int column) {}

    /**
     * Remove all highlights from chart.
     */
    public void clearHighlightedElements() {}

    public void setElementVisible(int row, int column, boolean visible) {};

    /**
     * Get the dataset associated with this chart or <code>null</code> if there the chart does not
     * have one.
     */
    public final DataTupleDataset getDataset() {
        if (getChart() == null) {
            return null;
        }

        Plot plot = getChart().getPlot();

        if (plot instanceof XYPlot) {
            XYPlot xyPlot = getChart().getXYPlot();

            if (xyPlot.getDatasetCount() == 1) {
                return (DataTupleDataset) xyPlot.getDataset();
            }
            else {
                // assume only 2 datasets / 2 axes
                return DataTupleXYDataset.merge((DataTupleXYDataset) xyPlot.getDataset(0),
                        (DataTupleXYDataset) xyPlot.getDataset(1));
            }
        }
        else if (plot instanceof CategoryPlot) {
            return (DataTupleDataset) ((CategoryPlot) plot).getDataset();
        }
        else {
            return null;
        }
    }

    // update the base class methods to put out images of a fixed size rather than the current size
    // of the chart in the gui
    @Override
    public final void doCopy() {
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        ChartTransferable selection = new ChartTransferable(getChart(), 1920 / 2, 1080 / 2);
        systemClipboard.setContents(selection, null);
    }

    public final void doCopyDataset() {
        DataTupleDataset data = getDataset();

        if (data != null) {
            StringWriter writer = new StringWriter(1024);

            try {
                CSVWriter.write(data, writer);

                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                systemClipboard.setContents(new StringSelection(writer.toString()), null);
            }
            catch (IOException ioe) {
                logger.error("error copying dataset", ioe);
            }
        }
    }

    @Override
    public final void doSaveAs() throws IOException {
        String directory = gui.getPreferences().get("lastSaveDirectory", "./");
        String filename = getSaveFileName() + ".png";
        File chartFile = new File(directory, filename);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(chartFile);

        ExtensionFileFilter filter = new ExtensionFileFilter(localizationResources.getString("PNG_Image_Files"), ".png");
        fileChooser.addChoosableFileFilter(filter);

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            chartFile = fileChooser.getSelectedFile();

            if (isEnforceFileExtensions()) {
                if (!chartFile.getName().endsWith(".png")) {
                    chartFile = new File(chartFile.getAbsolutePath() + ".png");
                }
            }

            ChartUtilities.saveChartAsPNG(chartFile, getChart(), 1920 / 2, 1080 / 2);
        }

        // save the last directory whether or not the file is actually saved
        gui.getPreferences().put("lastSaveDirectory", chartFile.getParentFile().getAbsolutePath());
    }

    public final void saveChart(String directory, String filename) {
        if ((filename == null) || "".equals(filename)) {
            filename = getSaveFileName() + ".png";
        }

        if (isEnforceFileExtensions()) {
            if (!filename.endsWith(".png")) {
                filename += ".png";
            }
        }

        File chartFile = new File(directory, filename);

        try {
            ChartUtilities.saveChartAsPNG(chartFile, getChart(), 1920 / 2, 1080 / 2);
        }
        catch (IOException ioe) {
            logger.error("could not save chart '" + getSaveFileName() + "' to directory '" + directory + "'", ioe);
        }
    }

    protected String getSaveFileName() {
        String title = getChart().getTitle().getText().replace('\n', ' ');

        if ((title == null) || "".equals(title)) {
            return "chart_" + this.hashCode();
        }
        else {
            return title;
        }
    }

    @Override
    protected JPopupMenu createPopupMenu(boolean properties, boolean copy, boolean save, boolean print, boolean zoom) {
        JPopupMenu popup = super.createPopupMenu(properties, copy, save, print, zoom);
        int n = 0;

        // find the existing 'Copy' menu item and add an option to copy chart data after that
        // rename the existing 'Save as...' item to 'Save Chart'
        for (MenuElement element : popup.getSubElements()) {
            JMenuItem item = (JMenuItem) element;

            if (item.getText().equals("Copy")) {
                JMenuItem copyData = new JMenuItem("Copy Chart Data");
                copyData.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        doCopyDataset();
                    }
                });

                // after separator, after copy => + 2
                popup.add(copyData, n + 2);
                popup.add(new JPopupMenu.Separator(), n + 3);
            }
            else if (item.getText().equals("Save as...")) {
                item.setText("Save Chart...");
            }

            n++;
        }

        return popup;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {}

    // disable mouse movement to avoid constantly searching for ChartEntity objects when there are
    // ChartMouseListeners
    // assume subclasses will not need the functionality (zooming / panning) provided in the super
    // class
    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}
}