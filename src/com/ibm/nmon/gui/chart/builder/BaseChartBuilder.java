package com.ibm.nmon.gui.chart.builder;

import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.Iterator;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;

import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;

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
 * Users of this class should first call {@link #initChart()} to initialize and set up the chart. Any subclass methods
 * should then be called to perform other setup or add data to the chart. Finally, {@link #getChart()} should be called
 * to retrieve the chart from the builder. Once <code>getChart()</code> is called, it <em>cannot</em> be called again
 * until <code>initChart()</code> is called.
 * </p>
 */
abstract class BaseChartBuilder<C extends BaseChartDefinition> {
    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    private Interval interval;
    private int granularity = GranularityHelper.DEFAULT_GRANULARITY;

    private List<ChartBuilderPlugin> plugins;

    protected JFreeChart chart;
    protected C definition;

    protected ChartFormatter formatter;

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
        if (definition == null) {
            throw new IllegalArgumentException("chart definition cannot be null");
        }

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
     * @throws IllegalStateException if {@link #initChart()} has not been called}
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

    public void setFormatter(ChartFormatter formatter) {
        if (formatter != null) {
            this.formatter = formatter;
        }
    }

    /**
     * Create a usable chart object. This method should not format the chart, it should only instantiate it.
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

        chart.setTitle(definition.getTitle());

        formatter.formatChart(chart);
    }

    protected final void addLegend() {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        LegendTitle legend = new LegendTitle(chart.getPlot());
        formatter.formatLegend(legend);

        chart.addLegend(legend);
    }

    private final void updateSubtitle() {
        TextTitle subtitle = (TextTitle) chart.getSubtitle(0);

        // short circuit if NONE or only 1 Statistic
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
                subtitle.setText(definition.getSubtitleNamingMode().getName(definition.getData().iterator().next(),
                        null, null, null, interval, granularity));
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
        subtitle.setText(getSubtitle(definition.getSubtitleNamingMode(), stats, dataSets, dataTypes, fields));

    }

    private String getSubtitle(NamingMode mode, Set<Statistic> stats, Set<DataSet> dataSets, Set<DataType> dataTypes,
            Set<String> fields) {

        // if Set is not unique for the given NamingMode, change it
        // default to a blank subtitle
        NamingMode actualMode = NONE;
        switch (mode) {
        case NONE: {}
        case STAT: {
            throw new IllegalStateException("NamingMode" + mode + " should already have been handled");
        }
        case HOST: {
            if (dataSets.size() == 1) {
                actualMode = HOST;
            }
            break;
        }
        case TYPE: {
            if (dataTypes.size() == 1) {
                actualMode = TYPE;
            }
            break;
        }
        case FIELD: {
            if (fields.size() == 1) {
                actualMode = FIELD;
            }
            break;
        }
        // for compound modes, both must be unique; if one part is unique, use that instead
        case HOST_TYPE: {
            if (dataTypes.size() == 1) {
                if (dataSets.size() == 1) {
                    actualMode = HOST_TYPE;
                }
                else {
                    actualMode = TYPE;
                }
            }
            else if (dataSets.size() == 1) {
                actualMode = HOST;
            }
            break;
        }
        case HOST_FIELD: {
            if (fields.size() == 1) {
                if (dataSets.size() == 1) {
                    actualMode = HOST_FIELD;
                }
                else {
                    actualMode = FIELD;
                }
            }
            else if (dataSets.size() == 1) {
                actualMode = HOST;
            }
            break;
        }
        case HOST_STAT: {
            if (stats.size() == 1) {
                if (dataSets.size() == 1) {
                    actualMode = HOST_STAT;
                }
                else {
                    actualMode = STAT;
                }
            }
            else if (dataSets.size() == 1) {
                actualMode = HOST;
            }
            break;
        }
        case TYPE_FIELD: {
            if (fields.size() == 1) {
                if (dataTypes.size() == 1) {
                    actualMode = TYPE_FIELD;
                }
                else {
                    actualMode = FIELD;
                }
            }
            else if (dataTypes.size() == 1) {
                actualMode = TYPE;
            }
            break;
        }
        case TYPE_STAT: {
            if (stats.size() == 1) {
                if (dataTypes.size() == 1) {
                    actualMode = TYPE_STAT;
                }
                else {
                    actualMode = STAT;
                }
            }
            else if (dataTypes.size() == 1) {
                actualMode = TYPE;
            }
            break;
        }
        case FIELD_STAT: {
            if (stats.size() == 1) {
                if (fields.size() == 1) {
                    actualMode = FIELD_STAT;
                }
                else {
                    actualMode = STAT;
                }
            }
            else if (fields.size() == 1) {
                actualMode = FIELD;
            }
            break;
        }
        case DATE: {
            // use the first DataDefinition
            // return here to avoid using HOST, TYPE or FIELD values when there is more than one value
            return DATE.getName(definition.getData().iterator().next(),
                    dataSets.size() == 1 ? dataSets.iterator().next() : null,
                    dataTypes.size() == 1 ? dataTypes.iterator().next() : null,
                    fields.size() == 1 ? fields.iterator().next() : null, interval, granularity);
        }
        }

        String subtitle = "";

        for (DataDefinition dataDefinition : definition.getData()) {
            String oldSubtitle = subtitle;

            // note that getName() ignores data, type, field, etc if not needed, so it is safe to pass the first element
            // in each Set
            // but, handle null values when no data selected due to bad report definitions
            Iterator<DataSet> ids = dataSets.iterator();
            DataSet set = ids.hasNext() ?  ids.next() : null;

            Iterator<DataType> idt = dataTypes.iterator();
            DataType type = idt.hasNext() ?  idt.next() : null;

            Iterator<String> ifd = fields.iterator();
            String field = ifd.hasNext() ?  ifd.next() : null;

            // the above switch logic has already set the NamingMode to the necessary value
            subtitle = actualMode.getName(dataDefinition, set, type, field, interval, granularity);

            // end after first successful rename
            if (!subtitle.equals(oldSubtitle)) {
                break;
            }
        }

        return subtitle;
    }
}
