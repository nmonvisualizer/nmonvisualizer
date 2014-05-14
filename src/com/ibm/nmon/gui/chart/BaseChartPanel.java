package com.ibm.nmon.gui.chart;

import org.slf4j.Logger;

import java.awt.Graphics;
import java.awt.Point;
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
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartTransferable;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryTextAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.CategoryItemEntity;

import org.jfree.ui.ExtensionFileFilter;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.chart.data.*;
import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.file.GUIFileChooser;
import com.ibm.nmon.util.CSVWriter;

public class BaseChartPanel extends ChartPanel implements PropertyChangeListener {
    private static final long serialVersionUID = -1342720624336005568L;

    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    protected final NMONVisualizerGui gui;

    private java.awt.Point clickLocation = null;

    private JMenuItem currentAnnotationMenu;
    private final JMenuItem annotateBar;
    private final JMenu annotateLine;

    protected BaseChartPanel(NMONVisualizerGui gui) {
        // With multiple charts for each ReportPanel and one report panel per DataSet, there could
        // be a large number of active charts. To keep memory usage down, disable using a buffered
        // image despite any performance benefit.
        // Unfortunately, chartBuffer in ChartPanel is private so there is no way to clear the
        // memory used when the chart is not being displayed.
        super(null, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MINIMUM_DRAW_WIDTH, DEFAULT_MINIMUM_DRAW_HEIGHT,
                DEFAULT_MAXIMUM_DRAW_WIDTH, DEFAULT_MAXIMUM_DRAW_HEIGHT, false, true, false, false, false, true);

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
        setEnforceFileExtensions(true);

        annotateBar = new JMenuItem("Annotate Bar");
        annotateBar.addActionListener(new AnnotateBarWithText());

        annotateLine = new JMenu("Annotate");

        JMenuItem item = new JMenuItem("Vertical Line");
        item.addActionListener(new BaseLineAnnotationAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double x = getGraphCoordinates()[0];

                ValueMarker marker = new ValueMarker(x);
                marker.setStroke(Styles.ANNOTATION_STROKE);
                marker.setPaint(Styles.ANNOTATION_COLOR);
                marker.setLabelFont(Styles.ANNOTATION_FONT);
                marker.setLabelPaint(Styles.ANNOTATION_COLOR);
                marker.setLabelOffset(new RectangleInsets(5, 5, 5, 5));
                marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

                if (getChart().getXYPlot().getDomainAxis() instanceof org.jfree.chart.axis.DateAxis) {
                    double range = getChart().getXYPlot().getDomainAxis().getUpperBound()
                            - getChart().getXYPlot().getDomainAxis().getLowerBound();

                    if (range > (86400 * 1000)) {
                        marker.setLabel(new java.text.SimpleDateFormat(Styles.DATE_FORMAT_STRING).format(x));
                    }
                    else {
                        marker.setLabel(new java.text.SimpleDateFormat(Styles.DATE_FORMAT_STRING_SHORT).format(x));
                    }
                }
                else {
                    marker.setLabel(Styles.NUMBER_FORMAT.format(x));
                }

                getChart().getXYPlot().addDomainMarker(marker);

                firePropertyChange("annotation", null, marker);
            }
        });
        annotateLine.add(item);

        item = new JMenuItem("Horizontal Line");
        item.addActionListener(new BaseLineAnnotationAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double y = getGraphCoordinates()[1];

                ValueMarker marker = new ValueMarker(y);
                marker.setLabel(Styles.NUMBER_FORMAT.format(y));
                marker.setStroke(Styles.ANNOTATION_STROKE);
                marker.setPaint(Styles.ANNOTATION_COLOR);
                marker.setLabelFont(Styles.ANNOTATION_FONT);
                marker.setLabelPaint(Styles.ANNOTATION_COLOR);
                marker.setLabelTextAnchor(TextAnchor.BASELINE_LEFT);

                getChart().getXYPlot().addRangeMarker(marker);

                firePropertyChange("annotation", null, marker);
            }
        });
        annotateLine.add(item);

        item = new JMenuItem("Text");
        item.addActionListener(new BaseLineAnnotationAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = JOptionPane.showInputDialog(BaseChartPanel.this.gui.getMainFrame(), "Annotation Text",
                        "Annotate Bar Chart", JOptionPane.QUESTION_MESSAGE);

                if (text != null) {
                    text = text.trim();

                    if ("".equals(text)) {
                        return;
                    }

                    double[] graphCoords = getGraphCoordinates();

                    XYTextAnnotation annotation = new XYTextAnnotation(text, graphCoords[0], graphCoords[1]);
                    annotation.setFont(Styles.ANNOTATION_FONT);
                    annotation.setPaint(Styles.ANNOTATION_COLOR);

                    getChart().getXYPlot().addAnnotation(annotation);

                    firePropertyChange("annotation", null, annotation);
                }
            }
        });

        annotateLine.add(item);
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

            removeCurrentAnnotationMenu();
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
     * @param row
     *            the chart's dataset row
     * @param column
     *            the chart's dataset column
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
            DataTupleDataset dataset = (DataTupleDataset) xyPlot.getDataset(0);

            if (xyPlot.getDatasetCount() == 1) {
                return dataset;
            }
            else {
                // assume only 2 datasets / 2 axes
                if (dataset instanceof DataTupleXYDataset) {
                    return ((DataTupleXYDataset) dataset).merge((DataTupleXYDataset) xyPlot.getDataset(1));
                }
                else if (dataset instanceof DataTupleHistogramDataset) {
                    return ((DataTupleHistogramDataset) dataset)
                            .merge((DataTupleHistogramDataset) xyPlot.getDataset(1));
                }
                else {
                    logger.warn("unknown DataTupleDataset class {}, returning null", dataset.getClass().getName());
                    return null;
                }
            }
        }
        else if (plot instanceof CategoryPlot) {
            CategoryPlot categoryPlot = (CategoryPlot) plot;
            DataTupleCategoryDataset dataset = (DataTupleCategoryDataset) categoryPlot.getDataset(0);

            if (categoryPlot.getDatasetCount() == 1) {
                return dataset;
            }
            else {
                return dataset.merge((DataTupleCategoryDataset) categoryPlot.getDataset(1));
            }
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
        String filename = validateSaveFileName(null);
        File chartFile = new File(directory, filename);

        GUIFileChooser fileChooser = new GUIFileChooser(gui, "Select Save Location");
        fileChooser.setSelectedFile(chartFile);

        ExtensionFileFilter filter = new ExtensionFileFilter(localizationResources.getString("PNG_Image_Files"), ".png");
        fileChooser.addChoosableFileFilter(filter);

        if (fileChooser.showDialog(this, "Save") == JFileChooser.APPROVE_OPTION) {
            chartFile = fileChooser.getSelectedFile();

            if (isEnforceFileExtensions()) {
                if (!chartFile.getName().endsWith(".png")) {
                    chartFile = new File(chartFile.getAbsolutePath() + ".png");
                }
            }

            if (chartFile.exists()) {
                int result = JOptionPane.showConfirmDialog(gui.getMainFrame(), "File '" + chartFile.getName()
                        + "' already exists.\nDo you want to overwrite it?", "Overwrite?",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (result != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            ChartUtilities.saveChartAsPNG(chartFile, getChart(), 1920 / 2, 1080 / 2);
        }

        // save the last directory whether or not the file is actually saved
        gui.getPreferences().put("lastSaveDirectory", chartFile.getParentFile().getAbsolutePath());
    }

    public final void saveChart(String directory, String filename) {
        filename = validateSaveFileName(filename);

        File chartFile = new File(directory, filename);

        try {
            ChartUtilities.saveChartAsPNG(chartFile, getChart(), 1920 / 2, 1080 / 2);
        }
        catch (IOException ioe) {
            logger.error("could not save chart '" + filename + "' to directory '" + directory + "'", ioe);
        }
    }

    public final void addAnnotations(List<Object> annotations) {
        if (getChart() == null) {
            return;
        }

        System.out.println("adding annotations");
        if (getChart().getPlot() instanceof CategoryPlot) {
            for (Object o : annotations) {
                if (o instanceof CategoryTextAnnotation) {
                    CategoryTextAnnotation annotation = (CategoryTextAnnotation) o;
System.out.println("\t" + annotation.getCategory());
                    getChart().getCategoryPlot().addAnnotation(annotation);
                }
            }
        }
        else if (getChart().getPlot() instanceof XYPlot) {
            for (Object o : annotations) {
                if (o instanceof XYTextAnnotation) {
                    XYTextAnnotation annotation = (XYTextAnnotation) o;
                    System.out.println("\t" + annotation.getX() + " " + annotation.getY());
                    getChart().getXYPlot().addAnnotation(annotation);
                }
                else if (o instanceof ValueMarker) {
                    ValueMarker marker = (ValueMarker) o;
                    System.out.println("\t" + marker.getLabel() + " " + marker.getValue());

                    getChart().getXYPlot().addDomainMarker(marker);
                }
            }
        }
    }

    protected String validateSaveFileName(String filename) {
        if ((filename == null) || "".equals(filename)) {
            String title = getChart().getTitle().getText();

            if ((title == null) || "".equals(title)) {
                filename = "chart_" + this.hashCode();
            }
            else {
                filename = title;
            }
        }

        if (isEnforceFileExtensions() && !filename.endsWith(".png")) {
            filename += ".png";
        }

        filename.replace('\n', ' ');

        return filename;
    }

    @Override
    protected JPopupMenu createPopupMenu(boolean properties, boolean copy, boolean save, boolean print, boolean zoom) {
        JPopupMenu popup = super.createPopupMenu(properties, copy, save, print, zoom);
        int n = 0;

        // find the existing 'Copy' menu item and add an option to copy chart data after that
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

            n++;
        }

        // create Save Chart item
        // note that the default 'Save as' item is no present since false was passed into the
        // BaseChartPanel constructor when creating this class' instance
        JMenuItem savePNG = new JMenuItem("Save Chart...");
        savePNG.setActionCommand("SAVE_AS_PNG");
        savePNG.addActionListener(this);

        popup.add(savePNG);

        return popup;
    }

    @Override
    protected void displayPopupMenu(int x, int y) {
        // always add Annotate menu for XYPlots; for CategoryPlots, only show if the mouse if over
        // an actual bar
        if (getChart().getPlot() instanceof CategoryPlot) {
            // find the CategoryItemEntity that matches the given x, y
            // assume there are not that many entities in the chart and this will be relatively
            // fast
            @SuppressWarnings("rawtypes")
            java.util.Iterator i = getChartRenderingInfo().getEntityCollection().iterator();

            boolean valid = false;

            while (i.hasNext()) {
                ChartEntity entity = (ChartEntity) i.next();

                if (entity.getClass() == CategoryItemEntity.class) {
                    CategoryItemEntity categoryEntity = (CategoryItemEntity) entity;

                    if (categoryEntity.getArea().contains(x, y)) {
                        ((AnnotateBarWithText) annotateBar.getActionListeners()[0]).categoryKey = (String) categoryEntity
                                .getColumnKey();
                        valid = true;
                        break;
                    }
                }
            }

            if (valid) {
                if (currentAnnotationMenu != annotateBar) {
                    getPopupMenu().addSeparator();
                    getPopupMenu().add(annotateBar);

                    currentAnnotationMenu = annotateBar;
                }
            }
            else {
                removeCurrentAnnotationMenu();
            }
        }
        else {
            if (currentAnnotationMenu != annotateLine) {
                removeCurrentAnnotationMenu();

                getPopupMenu().addSeparator();
                getPopupMenu().add(annotateLine);

                currentAnnotationMenu = annotateLine;
            }
        }

        if (currentAnnotationMenu != null) {

        }

        super.displayPopupMenu(x, y);
    }

    private void removeCurrentAnnotationMenu() {
        if (currentAnnotationMenu != null) {
            boolean removed = false;

            for (java.awt.Component c : getPopupMenu().getComponents()) {
                if (c == currentAnnotationMenu) {
                    getPopupMenu().remove(currentAnnotationMenu);
                    removed = true;
                    break;
                }
            }

            if (removed) {
                // remove separator
                getPopupMenu().remove(getPopupMenu().getComponentCount() - 1);

                currentAnnotationMenu = null;
            }
        }
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

    // track location of the mouse event for annotations
    @Override
    public void mousePressed(MouseEvent event) {
        if (event.isPopupTrigger()) {
            clickLocation = new Point(event.getX(), event.getY());
        }

        super.mousePressed(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (event.isPopupTrigger()) {
            clickLocation = new Point(event.getX(), event.getY());
        }

        super.mouseReleased(event);
    }

    @Override
    public void paintComponent(Graphics g) {
        if (logger.isDebugEnabled()) {
            long start = System.nanoTime();

            super.paintComponent(g);

            String title = "<no title>";

            if ((getChart() != null) && (getChart().getTitle()) != null) {
                title = getChart().getTitle().getText();
            }

            logger.debug("painted chart '{}' in {} ms", title, (System.nanoTime() - start) / 1000000.0d);
        }
        else {
            super.paintComponent(g);
        }
    }

    private final class AnnotateBarWithText implements ActionListener {
        String categoryKey = null;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (categoryKey == null) {
                return;
            }

            String text = JOptionPane.showInputDialog(gui.getMainFrame(), "Annotation Text", "Annotate Bar Chart",
                    JOptionPane.QUESTION_MESSAGE);

            if (text != null) {
                text = text.trim();

                if ("".equals(text)) {
                    return;
                }

                CategoryPlot categoryPlot = getChart().getCategoryPlot();

                double y = categoryPlot.getRangeAxis().java2DToValue(clickLocation.getY(),
                        getChartRenderingInfo().getPlotInfo().getDataArea(), categoryPlot.getRangeAxisEdge());

                if (y < categoryPlot.getRangeAxis().getLowerBound()) {
                    y = categoryPlot.getRangeAxis().getLowerBound();
                }
                if (y > categoryPlot.getRangeAxis().getUpperBound()) {
                    y = categoryPlot.getRangeAxis().getUpperBound();
                }

                CategoryTextAnnotation annotation = new CategoryTextAnnotation(text, categoryKey, y);
                annotation.setFont(Styles.ANNOTATION_FONT);
                annotation.setPaint(Styles.ANNOTATION_COLOR);

                getChart().getCategoryPlot().addAnnotation(annotation);

                firePropertyChange("annotation", null, annotation);
            }
        }
    }

    private abstract class BaseLineAnnotationAction implements ActionListener {
        protected final double[] getGraphCoordinates() {
            XYPlot xyPlot = getChart().getXYPlot();

            java.awt.geom.Rectangle2D dataArea = getChartRenderingInfo().getPlotInfo().getDataArea();

            double x = xyPlot.getDomainAxis().java2DToValue(clickLocation.getX(), dataArea, xyPlot.getDomainAxisEdge());
            double y = xyPlot.getRangeAxis().java2DToValue(clickLocation.getY(), dataArea, xyPlot.getRangeAxisEdge());

            if (x < xyPlot.getDomainAxis().getLowerBound()) {
                x = xyPlot.getDomainAxis().getLowerBound();
            }
            if (x > xyPlot.getDomainAxis().getUpperBound()) {
                x = xyPlot.getDomainAxis().getUpperBound();
            }

            if (y < xyPlot.getRangeAxis().getLowerBound()) {
                y = xyPlot.getRangeAxis().getLowerBound();
            }
            if (y > xyPlot.getRangeAxis().getUpperBound()) {
                y = xyPlot.getRangeAxis().getUpperBound();
            }

            return new double[] { x, y };
        }
    }
}