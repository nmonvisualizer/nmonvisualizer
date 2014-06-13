package com.ibm.nmon.gui.chart.builder;

import org.slf4j.Logger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import java.util.List;
import java.util.Set;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;

import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;

import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataTuple;
import com.ibm.nmon.data.DataType;

import com.ibm.nmon.analysis.Statistic;

import com.ibm.nmon.chart.definition.BaseChartDefinition;
import com.ibm.nmon.chart.definition.YAxisChartDefinition;

import com.ibm.nmon.data.definition.DataDefinition;

import com.ibm.nmon.data.definition.NamingMode;
import static com.ibm.nmon.data.definition.NamingMode.*;

import com.ibm.nmon.gui.chart.data.DataTupleDataset;

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
abstract class BaseChartBuilder<C extends BaseChartDefinition> {
    protected static final Font TITLE_FONT = new Font("null", Font.BOLD, 18);
    protected static final Font SUBTITLE_FONT = new Font("null", Font.PLAIN, 16);
    protected static final Font LABEL_FONT = new Font("null", Font.BOLD, 16);
    protected static final Font AXIS_FONT = new Font("null", Font.PLAIN, 14);
    protected static final Font LEGEND_FONT = new Font("null", Font.PLAIN, 14);

    protected static final Color GRID_COLOR = Color.LIGHT_GRAY;
    protected static final BasicStroke GRID_LINES = new BasicStroke(0.5f, 0, 0, 1.0f, new float[] { 5.0f, 2.0f }, 0);

    protected static final java.awt.Color OUTLINE_COLOR = new java.awt.Color(0xCCCCCC);
    protected static final BasicStroke OUTLINE_STROKE = new BasicStroke(3);

    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    private Interval interval;
    private int granularity = GranularityHelper.DEFAULT_GRANULARITY;

    private List<ChartBuilderPlugin> plugins;

    protected JFreeChart chart;
    protected C definition;

    protected BaseChartBuilder() {
        interval = Interval.DEFAULT;
    }

    public Interval getInterval() {
        return interval;
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
    public final void initChart(C definition) {
        this.definition = definition;
        chart = createChart();
        chart.setSubtitles(java.util.Collections.singletonList(new TextTitle()));

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
     * @throws IllegalStateException
     *             if {@link #initChart()} has not been called}
     */
    public final JFreeChart getChart() {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        updateSubtitle();

        try {
            return chart;
        }
        finally {
            chart = null;
            definition = null;
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
        ((TextTitle) chart.getSubtitle(0)).setFont(SUBTITLE_FONT);
        ((TextTitle) chart.getSubtitle(0)).setPadding(new RectangleInsets(0, 0, 0, 0));

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

    private final void updateSubtitle() {
        TextTitle subtitle = (TextTitle) chart.getSubtitle(0);

        if (definition.getSubtitleNamingMode() == NONE) {
            subtitle.setText("");
            return;
        }

        Set<Statistic> stats = new java.util.HashSet<Statistic>();

        for (DataDefinition dataDefinition : definition.getData()) {
            stats.add(dataDefinition.getStatistic());
        }

        if (definition.getSubtitleNamingMode() == STAT) {
            if (stats.size() == 1) {
                subtitle.setText(stats.iterator().next().toString());
            }
            else {
                subtitle.setText("");
            }

            return;
        }

        // get the chart Datasets
        Plot plot = chart.getPlot();
        Iterable<DataTuple> allTuples = null;

        DataTupleDataset data1 = null;
        DataTupleDataset data2 = null;

        boolean combineDataSets = false;

        if (definition instanceof YAxisChartDefinition) {
            combineDataSets = ((YAxisChartDefinition) definition).hasSecondaryYAxis();
        }

        if (plot instanceof XYPlot) {
            data1 = ((DataTupleDataset) ((XYPlot) plot).getDataset(0));

            if (combineDataSets) {
                data2 = ((DataTupleDataset) ((XYPlot) plot).getDataset(1));
            }
        }
        else {
            // currently only XYPlot and CategoryPlot are used
            data1 = ((DataTupleDataset) ((CategoryPlot) plot).getDataset(0));

            if (combineDataSets) {
                data2 = ((DataTupleDataset) ((CategoryPlot) plot).getDataset(1));
            }
        }

        allTuples = data1.getAllTuples();

        // combine the tuples if needed
        if (combineDataSets) {
            Set<DataTuple> combined = new java.util.HashSet<DataTuple>();

            for (DataTuple tuple : allTuples) {
                combined.add(tuple);
            }

            allTuples = data2.getAllTuples();

            for (DataTuple tuple : allTuples) {
                combined.add(tuple);
            }

            allTuples = combined;
        }

        // get the unique set of each DataSet, DataType and field
        Set<DataSet> dataSets = new java.util.HashSet<DataSet>();
        Set<DataType> dataTypes = new java.util.HashSet<DataType>();
        Set<String> fields = new java.util.HashSet<String>();

        for (DataTuple t : allTuples) {
            dataSets.add(t.getDataSet());
            dataTypes.add(t.getDataType());
            fields.add(t.getField());
        }

        // now set the subtitle based on the NamingMode
        NamingMode mode = definition.getSubtitleNamingMode();

        // only set if there is a single data set, type of field
        if (mode == HOST) {
            if (dataSets.size() == 1) {
                subtitle.setText(dataSets.iterator().next().getHostname());
            }
            else {
                subtitle.setText("");
            }
        }
        else if (mode == TYPE) {
            if (dataTypes.size() == 1) {
                subtitle.setText(dataTypes.iterator().next().toString());
            }
            else {
                subtitle.setText("");
            }
        }
        else if (mode == FIELD) {
            if (fields.size() == 1) {
                subtitle.setText(fields.iterator().next());
            }
            else {
                subtitle.setText("");
            }
        }
        // for compound modes, both must be unique; if one part is unique, use that instead
        else if (mode == HOST_TYPE) {
            if (dataTypes.size() == 1) {
                if (dataSets.size() == 1) {
                    subtitle.setText(dataSets.iterator().next().getHostname() + SEPARATOR
                            + dataTypes.iterator().next().toString());
                }
                else {
                    subtitle.setText(dataTypes.iterator().next().toString());
                }
            }
            else {
                if (dataSets.size() == 1) {
                    subtitle.setText(dataSets.iterator().next().getHostname());
                }
                else {
                    subtitle.setText("");
                }
            }
        }
        else if (mode == HOST_FIELD) {
            if (fields.size() == 1) {
                if (dataSets.size() == 1) {
                    subtitle.setText(dataSets.iterator().next().getHostname() + SEPARATOR + fields.iterator().next());
                }
                else {
                    subtitle.setText(fields.iterator().next());
                }
            }
            else {
                if (dataSets.size() == 1) {
                    subtitle.setText(dataSets.iterator().next().getHostname());
                }
                else {
                    subtitle.setText("");
                }
            }
        }
        else if (mode == HOST_STAT) {
            if (stats.size() == 1) {
                if (dataSets.size() == 1) {
                    subtitle.setText(dataSets.iterator().next().getHostname() + SEPARATOR
                            + stats.iterator().next().toString());
                }
                else {
                    subtitle.setText(stats.iterator().next().toString());
                }
            }
            else {
                if (dataSets.size() == 1) {
                    subtitle.setText(dataSets.iterator().next().getHostname());
                }
                else {
                    subtitle.setText("");
                }
            }
        }
        else if (mode == TYPE_FIELD) {
            if (fields.size() == 1) {
                if (dataTypes.size() == 1) {
                    subtitle.setText(dataTypes.iterator().next().toString() + SEPARATOR + fields.iterator().next());
                }
                else {
                    subtitle.setText(fields.iterator().next());
                }
            }
            else {
                if (dataTypes.size() == 1) {
                    subtitle.setText(dataTypes.iterator().next().toString());
                }
                else {
                    subtitle.setText("");
                }
            }
        }
        else if (mode == TYPE_STAT) {
            if (stats.size() == 1) {
                if (dataTypes.size() == 1) {
                    subtitle.setText(dataTypes.iterator().next().toString() + SEPARATOR
                            + stats.iterator().next().toString());
                }
                else {
                    subtitle.setText(stats.iterator().next().toString());
                }
            }
            else {
                if (dataTypes.size() == 1) {
                    subtitle.setText(stats.iterator().next().toString());
                }
                else {
                    subtitle.setText("");
                }
            }
        }
        else if (mode == FIELD_STAT) {
            if (stats.size() == 1) {
                if (fields.size() == 1) {
                    subtitle.setText(fields.iterator().next() + SEPARATOR + stats.iterator().next().toString());
                }
                else {
                    subtitle.setText(stats.iterator().next().toString());
                }
            }
            else {
                if (fields.size() == 1) {
                    subtitle.setText(fields.iterator().next());
                }
                else {
                    subtitle.setText("");
                }
            }
        }
    }
}
