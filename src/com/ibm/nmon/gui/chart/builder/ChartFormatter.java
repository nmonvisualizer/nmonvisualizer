package com.ibm.nmon.gui.chart.builder;

import java.awt.Paint;
import java.awt.Color;
import java.awt.Font;

import java.awt.BasicStroke;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CategoryPlot;

import org.jfree.chart.axis.Axis;

import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.LegendTitle;

import org.jfree.chart.plot.DefaultDrawingSupplier;

import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.annotations.Annotation;
import org.jfree.chart.annotations.TextAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.Marker;

import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.ibm.nmon.gui.Styles;
import com.ibm.nmon.gui.chart.annotate.AnnotationCache;

/**
 * <p>
 * Responsible for formatting all parts of a chart.
 * </p>
 * <p>
 * All setters in this class accept <code>null</code> to indicate a default value should be used.
 * </p>
 *
 * @see BaseChartBuilder#formatChart()
 */
public final class ChartFormatter {
    // default fonts for all charts; not configurable
    private static final Font TITLE_FONT = new Font("null", Font.BOLD, 18);
    private static final Font SUBTITLE_FONT = new Font("null", Font.PLAIN, 16);
    private static final Font LABEL_FONT = new Font("null", Font.BOLD, 16);
    private static final Font AXIS_FONT = new Font("null", Font.PLAIN, 14);
    private static final Font LEGEND_FONT = new Font("null", Font.PLAIN, 14);

    // default outline for bars; not configurable
    private static final Color OUTLINE_COLOR = new java.awt.Color(0xCCCCCC);

    // default, configurable colors for charts
    private static final Color DEFAULT_BACKGROUND = Color.WHITE;
    private static final Color DEFAULT_TEXT = Color.BLACK;
    private static final Color DEFAULT_ELEMENTS = Color.BLACK;
    private static final Color DEFAULT_GRID = Color.LIGHT_GRAY;
    private static final Color DEFAULT_ANNOTATION = new Color(0x222266);
    private static final Paint[] DEFAULT_SERIES;

    // line styles; not configurable
    private static final BasicStroke GRID_LINES = new BasicStroke(0.5f, 0, 0, 1.0f, new float[] { 5.0f, 2.0f }, 0);
    private static final BasicStroke OUTLINE_STROKE = new BasicStroke(3);
    private static final BasicStroke ANNOTATION_STROKE = new BasicStroke(.6f, 0, 0, 1.0f, new float[] { 1, 2, 5, 2 },
            5);

    private static final Font ANNOTATION_FONT = Styles.DEFAULT;

    // remove bright yellow from default sequence
    static {
        int l = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE.length;
        Paint[] temp = new Paint[l - 1];
        int n = 0;

        for (int i = 0; i < l; i++) {
            if (i != 3) {
                temp[n++] = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE[i];
            }
        }

        DEFAULT_SERIES = temp;
    }

    private Color background;
    private Color plotBackground;
    private Color textColor;
    private Color elementColor;
    private Color gridLineColor;
    private Color annotationColor;
    private Paint[] seriesColors;

    /**
     * Creates a formatter with default values.
     */
    public ChartFormatter() {
        this.background = DEFAULT_BACKGROUND;
        this.plotBackground = DEFAULT_BACKGROUND;
        this.textColor = DEFAULT_TEXT;
        this.elementColor = DEFAULT_ELEMENTS;
        this.gridLineColor = DEFAULT_GRID;
        this.annotationColor = DEFAULT_ANNOTATION;
        this.seriesColors = DEFAULT_SERIES;
    }

    /**
     * Set the color of the background for the entire chart.
     */
    public void setBackground(Color background) {
        if (background == null) {
            this.background = DEFAULT_BACKGROUND;
        }
        else {
            this.background = background;
        }
    }

    /**
     * Set the background of the plot where the data is actually drawn.
     */
    public void setPlotBackground(Color plotBackground) {
        if (plotBackground == null) {
            this.plotBackground = DEFAULT_BACKGROUND;
        }
        else {
            this.plotBackground = plotBackground;
        }
    }

    /**
     * Set the color of all text on the chart, except for numbers and time on the chart axes.
     */
    public void setTextColor(Color textColor) {
        if (textColor == null) {
            this.textColor = DEFAULT_TEXT;
        }
        else {
            this.textColor = textColor;
        }
    }

    /**
     * Set the color of the chart elements, including axes, axes values and tick marks.
     */
    public void setElementColor(Color elementsColor) {
        if (elementsColor == null) {
            this.elementColor = DEFAULT_ELEMENTS;
        }
        else {
            this.elementColor = elementsColor;
        }
    }

    /**
     * Set the color of the chart grid lines.
     */
    public void setGridLineColor(Color gridLineColor) {
        if (gridLineColor == null) {
            this.gridLineColor = DEFAULT_GRID;
        }
        else {
            this.gridLineColor = gridLineColor;
        }
    }

    /**
     * Set the color of the annotation text and lines.
     */
    public void setAnnotationColor(Color annotationColor) {
        if (annotationColor == null) {
            this.annotationColor = DEFAULT_ANNOTATION;
        }
        else {
            this.annotationColor = annotationColor;
        }
    }

    /**
     * Set the colors used by lines and bars on the chart. The values in the given array will be cycled for series on
     * the same chart. Setting a single value will cause all lines or bars to be the same color for all data series.
     */
    public void setSeriesColors(Color[] seriesColors) {
        if (seriesColors == null) {
            this.seriesColors = DEFAULT_SERIES;
        }
        else {
            this.seriesColors = java.util.Arrays.copyOf(seriesColors, seriesColors.length);
        }
    }

    public Font getAxisFont() {
        return AXIS_FONT;
    }

    void formatChart(JFreeChart chart) {
        chart.getTitle().setFont(TITLE_FONT);
        ((TextTitle) chart.getSubtitle(0)).setFont(SUBTITLE_FONT);
        ((TextTitle) chart.getSubtitle(0)).setPadding(new RectangleInsets(0, 0, 0, 0));

        chart.getTitle().setPaint(textColor);
        ((TextTitle) chart.getSubtitle(0)).setPaint(textColor);

        Plot plot = chart.getPlot();

        // chart has no outline but a little padding
        plot.setOutlineStroke(null);
        chart.setPadding(new RectangleInsets(5, 5, 5, 5));

        chart.getPlot().setDrawingSupplier(new DefaultDrawingSupplier(seriesColors,
                DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE, DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE, DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));

        chart.setBackgroundPaint(background);
        chart.getPlot().setBackgroundPaint(plotBackground);

        if (plot instanceof XYPlot) {
            XYPlot xyPlot = (XYPlot) plot;

            for (int i = 0; i < xyPlot.getRangeAxisCount(); i++) {
                formatAxis(xyPlot.getRangeAxis(i));
            }

            formatAxis(xyPlot.getDomainAxis());

            xyPlot.setRangeGridlinePaint(gridLineColor);
            xyPlot.setDomainGridlinePaint(gridLineColor);
            xyPlot.setRangeGridlineStroke(GRID_LINES);
        }
        else if (plot instanceof CategoryPlot) {
            CategoryPlot categoryPlot = (CategoryPlot) plot;

            for (int i = 0; i < categoryPlot.getRangeAxisCount(); i++) {
                formatAxis(categoryPlot.getRangeAxis(i));
            }

            formatAxis(categoryPlot.getDomainAxis());

            categoryPlot.setRangeGridlinePaint(gridLineColor);
            categoryPlot.setDomainGridlinePaint(gridLineColor);
            categoryPlot.setRangeGridlineStroke(GRID_LINES);
        }
    }

    private void formatAxis(Axis axis) {
        axis.setLabelFont(LABEL_FONT);
        axis.setTickLabelFont(AXIS_FONT);

        axis.setLabelPaint(textColor);
        axis.setAxisLinePaint(elementColor);
        axis.setTickLabelPaint(elementColor);
        axis.setTickMarkPaint(elementColor);
    }

    void formatRenderer(BarRenderer renderer) {
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter(new GradientPainters.GradientBarPainter());

        renderer.setBaseOutlineStroke(OUTLINE_STROKE);
        renderer.setBaseOutlinePaint(OUTLINE_COLOR);
    }

    void formatRenderer(XYBarRenderer renderer) {
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter(new GradientPainters.GradientXYBarPainter());

        renderer.setMargin(0.15d);

        renderer.setBaseOutlineStroke(OUTLINE_STROKE);
        renderer.setBaseOutlinePaint(OUTLINE_COLOR);
    }

    void formatLegend(LegendTitle legend) {
        legend.setItemFont(LEGEND_FONT);
        legend.setItemPaint(textColor);
        legend.setBorder(0, 0, 0, 0);
        legend.setPosition(RectangleEdge.BOTTOM);

        legend.setItemLabelPadding(new RectangleInsets(5, 5, 5, 5));
    }

    public void formatAnnotation(TextAnnotation annotation) {
        annotation.setFont(ANNOTATION_FONT);
        annotation.setPaint(annotationColor);
    }

    public void formatAnnotation(XYTextAnnotation annotation) {
        annotation.setFont(ANNOTATION_FONT);
        annotation.setPaint(annotationColor);
    }

    public void formatMarker(Marker marker, boolean horizontal, int offset) {
        marker.setStroke(ANNOTATION_STROKE);
        marker.setPaint(annotationColor);
        marker.setLabelFont(ANNOTATION_FONT);
        marker.setLabelPaint(annotationColor);

        if (horizontal) {
            // 50 to move data to the right of the x axis
            marker.setLabelOffset(new RectangleInsets(-5, offset + 50, 5, 5));
            marker.setLabelTextAnchor(TextAnchor.TOP_CENTER);
        }
        else {
            // -5 to move to right of line
            marker.setLabelOffset(new RectangleInsets(offset + 5, -5, 5, 5));
            marker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
        }
    }

    public void reformatAnnotations() {
        for (Annotation a : AnnotationCache.getAnnotations()) {
            if (a instanceof TextAnnotation) {
                formatAnnotation((TextAnnotation) a);
            }
            else if (a instanceof XYTextAnnotation) {
                formatAnnotation((XYTextAnnotation) a);
            }
        }

        for (Marker m : AnnotationCache.getMarkers()) {
            boolean horizontal = m.getLabelTextAnchor() == TextAnchor.TOP_CENTER;

            // match offsets adjustments in formatMarker()
            if (horizontal) {
                formatMarker(m, true, (int) m.getLabelOffset().getLeft() - 50);
            }
            else {
                formatMarker(m, false, (int) m.getLabelOffset().getTop() - 5);
            }
        }
    }
}
