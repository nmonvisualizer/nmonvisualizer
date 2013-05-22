package com.ibm.nmon.gui.chart.builder;

import org.slf4j.Logger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;

import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.util.GranularityHelper;


/**
 * <p>
 * Base class that defines a framework for creating {@link JFreeChart JFreeChart charts}.
 * </p>
 * 
 * <p>
 * Users of this class should first call {@link #initChart()} to initialize and set up the chart.
 * Any subclass methods should then be called to perform other setup or add data to the chart.
 * Finally, {@link #getChart()} should be called to retrieve the chart from the builder. Once
 * <code>getChart()</code> is called, it <em>cannot</em> be called again until
 * <code>initChart()</code> is called.
 * </p>
 */
abstract class BaseChartBuilder {
    protected static final Font TITLE_FONT = new Font("null", Font.BOLD, 18);
    protected static final Font LABEL_FONT = new Font("null", Font.BOLD, 16);
    protected static final Font AXIS_FONT = new Font("null", Font.PLAIN, 14);
    protected static final Font LEGEND_FONT = new Font("null", Font.PLAIN, 14);

    protected static final Color GRID_COLOR = Color.LIGHT_GRAY;
    protected static final BasicStroke GRID_LINES = new BasicStroke(0.5f, 0, 0, 1.0f, new float[] { 5.0f, 2.0f }, 0);

    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    protected Interval interval;
    protected int granularity = GranularityHelper.DEFAULT_GRANULARITY;

    protected JFreeChart chart;

    private List<ChartBuilderPlugin> plugins;

    protected BaseChartBuilder() {
        interval = Interval.DEFAULT;
    }

    public final void setInterval(Interval interval) {
        this.interval = interval;
    }

    public final int getGranularity() {
        return granularity;
    }

    public final void setGranularity(int granularity) {
        if (granularity < 1) {
            throw new IllegalArgumentException("granularity must be greater than 1");
        }

        this.granularity = granularity;
    }

    /**
     * Creates the chart, formats it and calls any {@link ChartBuilderPlugin plugins}.
     */
    public final void initChart() {
        chart = createChart();

        formatChart();

        if (plugins != null) {
            for (ChartBuilderPlugin plugin : plugins) {
                plugin.configureChart(chart);
            }
        }
    }

    /**
     * Retrieve the chart from the builder.
     * 
     * @throws IllegalStateException if {@link #initChart()} has not been called}
     */
    public final JFreeChart getChart() {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        try {
            return chart;
        }
        finally {
            chart = null;
        }
    }

    public final void addPlugin(ChartBuilderPlugin plugin) {
        if (chart != null) {
            throw new IllegalStateException("plugins must be added before initChart() is called");
        }

        if (plugins == null) {
            plugins = new java.util.ArrayList<ChartBuilderPlugin>(2);
        }

        plugins.add(plugin);
    }

    /**
     * Create a usable chart object. This method should not format the chart, it should only
     * instantiate it.
     * 
     * @see #formatChart()
     */
    protected abstract JFreeChart createChart();

    /**
     * Apply formatting to the chart created by {@link #createChart()}.
     */
    protected void formatChart() {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        chart.getTitle().setFont(TITLE_FONT);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setPadding(new RectangleInsets(5, 5, 5, 5));

        Plot plot = chart.getPlot();

        // chart has no outline on a white background
        plot.setOutlineStroke(null);
        plot.setBackgroundPaint(Color.WHITE);
    }

    protected final void addLegend() {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        LegendTitle legend = new LegendTitle(chart.getPlot());
        legend.setItemFont(LEGEND_FONT);
        legend.setBorder(0, 0, 0, 0);
        legend.setBackgroundPaint(Color.WHITE);
        legend.setPosition(RectangleEdge.BOTTOM);

        RectangleInsets padding = new RectangleInsets(5, 5, 5, 5);
        legend.setItemLabelPadding(padding);

        chart.addLegend(legend);
    }
}
