package com.ibm.nmon.gui.chart;

import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.beans.PropertyChangeEvent;

import java.util.List;
import java.util.TimeZone;

import javax.swing.JFrame;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartMouseEvent;

import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import org.jfree.chart.annotations.Annotation;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.entity.LegendItemEntity;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.chart.annotate.DomainValueMarker;
import com.ibm.nmon.gui.chart.annotate.RangeValueMarker;

import com.ibm.nmon.gui.chart.builder.LineChartBuilder;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

public class LineChartPanel extends BaseChartPanel implements ChartMouseListener {
    private static final long serialVersionUID = 7999499157941027546L;

    private final BasicStroke SELECTED_STROKE = new BasicStroke(5);

    public LineChartPanel(NMONVisualizerGui gui, JFrame parent) {
        super(gui, parent);

        addChartMouseListener(this);

    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            if (enabled) {
                gui.addPropertyChangeListener("timeZone", this);
                gui.addPropertyChangeListener("chartRelativeTime", this);

                setAxisTimeZone(gui.getDisplayTimeZone());
                setRelativeAxis(gui.getBooleanProperty("chartRelativeTime"));
            }
            else {
                gui.removePropertyChangeListener("timeZone", this);
                gui.removePropertyChangeListener("chartRelativeTime", this);
            }

            super.setEnabled(enabled);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("chartRelativeTime".equals(evt.getPropertyName())) {
            setRelativeAxis((Boolean) evt.getNewValue());
        }
        else if ("timeZone".equals(evt.getPropertyName())) {
            setAxisTimeZone((TimeZone) evt.getNewValue());
        }
    }

    // note that these methods assume the chart contains an XYPlot rendered by an XYItemRenderer
    // these are the expected values for all charts created by LineChartBuilder
    public final void highlightElement(int row, int column) {
        if (getChart() != null) {
            if ((row >= 0) && (row < getChart().getXYPlot().getDataset().getSeriesCount())) {
                XYItemRenderer renderer = getChart().getXYPlot().getRenderer();
                renderer.setSeriesStroke(row, SELECTED_STROKE);
            }
        }
    }

    @Override
    public final void clearHighlightedElements() {
        if (getChart() != null) {
            ((AbstractRenderer) getChart().getXYPlot().getRenderer()).clearSeriesStrokes(false);
        }
    }

    @Override
    public void setElementVisible(int row, int column, boolean visible) {
        if (getChart() != null) {
            if ((row >= 0) && (row < getChart().getXYPlot().getDataset().getSeriesCount())) {
                getChart().getXYPlot().getRenderer().setSeriesVisible(row, visible);
            }
        }
    }

    @Override
    public void addAnnotations(List<Annotation> annotations) {
        if (getChart() != null) {
            XYPlot plot = getChart().getXYPlot();

            plot.clearAnnotations();

            for (Annotation a : annotations) {
                if (a instanceof XYAnnotation) {
                    XYAnnotation annotation = (XYAnnotation) a;
                    plot.addAnnotation(annotation);
                }
            }
        }
    }

    @Override
    public void addMarkers(List<Marker> markers) {
        if (getChart() != null) {
            XYPlot plot = getChart().getXYPlot();

            plot.clearDomainMarkers();
            plot.clearRangeMarkers();

            for (Marker marker : markers) {
                if (marker instanceof RangeValueMarker) {
                    plot.addRangeMarker(marker);
                }
                else if (marker instanceof DomainValueMarker) {
                    plot.addDomainMarker(marker);
                }
            }
        }
    }

    @Override
    protected JPopupMenu createPopupMenu(boolean properties, boolean copy, boolean save, boolean print, boolean zoom) {
        JPopupMenu popupMenu = super.createPopupMenu(properties, copy, save, print, zoom);

        JMenu annotate = new JMenu("Annotate");

        JMenuItem item = new JMenuItem("Vertical Line");
        item.addActionListener(new AnnotateLineAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ValueMarker marker = createVerticalMarker(null);

                getChart().getXYPlot().addDomainMarker(marker);

                firePropertyChange("annotation", null, marker);
            }
        });
        annotate.add(item);

        item = new JMenuItem("Vertical Line with Text");
        item.addActionListener(new AnnotateLineAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = getAnnotationText();

                ValueMarker marker = createVerticalMarker(text);

                getChart().getXYPlot().addDomainMarker(marker);

                firePropertyChange("annotation", null, marker);
            }
        });
        annotate.add(item);

        item = new JMenuItem("Horizontal Line");
        item.addActionListener(new AnnotateLineAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ValueMarker marker = createHorizontalMarker(null);

                getChart().getXYPlot().addRangeMarker(marker);

                firePropertyChange("annotation", null, marker);
            }
        });
        annotate.add(item);

        item = new JMenuItem("Horizontal Line with Text");
        item.addActionListener(new AnnotateLineAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = getAnnotationText();

                ValueMarker marker = createHorizontalMarker(text);

                getChart().getXYPlot().addRangeMarker(marker);

                firePropertyChange("annotation", null, marker);
            }
        });
        annotate.add(item);

        item = new JMenuItem("Text");
        item.addActionListener(new AnnotateLineAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = getAnnotationText();

                if (text != null) {
                    getChart().getXYPlot().addAnnotation(createAnnotation(text));

                    firePropertyChange("annotation", null, text);
                }
            }
        });

        annotate.add(item);

        popupMenu.addSeparator();
        popupMenu.add(annotate);

        return popupMenu;
    }

    @Override
    public final void chartMouseClicked(ChartMouseEvent event) {
        int series = -1;

        ChartEntity entity = event.getEntity();

        if (entity == null) {
            return;
        }

        // users can click on either the line or the legend
        // regardless, figure out the series index
        if (entity.getClass() == XYItemEntity.class) {
            series = ((XYItemEntity) event.getEntity()).getSeriesIndex();
        }
        else if (entity.getClass() == LegendItemEntity.class) {
            LegendItemEntity legendEntity = (LegendItemEntity) event.getEntity();
            XYDataset dataset = (XYDataset) legendEntity.getDataset();

            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                if (dataset.getSeriesKey(i).equals(legendEntity.getSeriesKey())) {
                    series = i;
                    break;
                }
            }
        }

        if (series != -1) {
            XYItemRenderer renderer = getChart().getXYPlot().getRenderer();
            Stroke oldHighlight = renderer.getSeriesStroke(series);

            // clear existing highlights
            ((AbstractRenderer) getChart().getXYPlot().getRenderer()).clearSeriesStrokes(false);

            // toggle series stroke
            if (oldHighlight != SELECTED_STROKE) {
                renderer.setSeriesStroke(series, SELECTED_STROKE);

                firePropertyChange("highlightedLine", null, series);
            }
            else {
                renderer.setSeriesStroke(series, null);

                firePropertyChange("highlightedLine", series, null);
            }

            // assume whatever fired the event will repaint the chart
        }
    }

    @Override
    public final void chartMouseMoved(ChartMouseEvent event) {}

    private void setAxisTimeZone(TimeZone timeZone) {
        if (getChart() != null) {
            XYPlot plot = getChart().getXYPlot();

            if (plot.getDomainAxis() instanceof DateAxis) {
                ((DateAxis) plot.getDomainAxis()).setTimeZone(timeZone);
            }
        }
    }

    private void setRelativeAxis(boolean relative) {
        if (relative) {
            LineChartBuilder.setRelativeAxis(getChart(), gui.getMinSystemTime());
        }
        else {
            LineChartBuilder.setAbsoluteAxis(getChart());
        }
    }

    public abstract class AnnotateLineAction implements ActionListener {
        protected final ValueMarker createVerticalMarker(String text) {
            double x = getGraphCoordinates()[0];

            ValueMarker marker = new DomainValueMarker(x);
            marker.setLabelOffset(new RectangleInsets(5, 5, 5, 5));
            marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
            formatMarker(marker);

            if (text == null) {
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
            }
            else {
                marker.setLabel(text);
            }

            return marker;
        }

        protected final ValueMarker createHorizontalMarker(String text) {
            double y = getGraphCoordinates()[1];

            ValueMarker marker = new RangeValueMarker(y);
            marker.setLabelTextAnchor(TextAnchor.BASELINE_LEFT);
            formatMarker(marker);

            if (text == null) {
                marker.setLabel(Styles.NUMBER_FORMAT.format(y));
            }
            else {
                marker.setLabel(text);
            }
            return marker;
        }

        protected final XYTextAnnotation createAnnotation(String text) {
            double[] graphCoords = getGraphCoordinates();

            XYTextAnnotation annotation = new XYTextAnnotation(text, graphCoords[0], graphCoords[1]);
            annotation.setFont(Styles.ANNOTATION_FONT);
            annotation.setPaint(Styles.ANNOTATION_COLOR);

            return annotation;
        }

        private final double[] getGraphCoordinates() {
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

        private final void formatMarker(ValueMarker marker) {
            marker.setStroke(Styles.ANNOTATION_STROKE);
            marker.setPaint(Styles.ANNOTATION_COLOR);
            marker.setLabelFont(Styles.ANNOTATION_FONT);
            marker.setLabelPaint(Styles.ANNOTATION_COLOR);
        }
    }
}
