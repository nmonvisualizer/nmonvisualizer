package com.ibm.nmon.gui.chart.builder;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;

import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;

import org.jfree.chart.util.RelativeDateFormat;
import org.jfree.util.UnitType;

import org.jfree.data.time.FixedMillisecond;

import org.jfree.data.xy.XYDataset;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataTuple;

import com.ibm.nmon.data.definition.DataDefinition;
import com.ibm.nmon.data.definition.NamingMode;
import com.ibm.nmon.gui.chart.data.DataTupleXYDataset;

import com.ibm.nmon.chart.definition.LineChartDefinition;

public class LineChartBuilder extends BaseChartBuilder {
    private boolean stacked = false;
    private boolean hasYAxis2 = false;

    public LineChartBuilder() {
        super();
    }

    public void initChart(LineChartDefinition definition) {
        stacked = definition.isStacked();
        hasYAxis2 = definition.hasSecondaryYAxis();

        initChart();

        chart.setTitle(definition.getTitle());

        chart.getXYPlot().getRangeAxis().setLabel(definition.getYAxisLabel());

        if (hasYAxis2) {
            chart.getXYPlot().getRangeAxis(1).setLabel(definition.getSecondaryYAxisLabel());
        }

        if ("".equals(definition.getXAxisLabel())) {
            chart.getXYPlot().getDomainAxis().setLabel("Time");
        }
        else {
            chart.getXYPlot().getDomainAxis().setLabel(definition.getXAxisLabel());
        }

        if (definition.usePercentYAxis()) {
            LineChartBuilder.setPercentYAxis(chart);
        }
    }

    protected JFreeChart createChart() {
        DateAxis timeAxis = new DateAxis();

        NumberAxis valueAxis = new NumberAxis();
        valueAxis.setAutoRangeIncludesZero(true);

        DataTupleXYDataset dataset = new DataTupleXYDataset(stacked);

        XYPlot plot = null;

        if (stacked) {
            StackedXYAreaRenderer2 renderer = new StackedXYAreaRenderer2();
            renderer.setBaseSeriesVisible(true, false);

            plot = new XYPlot(dataset, timeAxis, valueAxis, renderer);
        }
        else {
            StandardXYItemRenderer renderer = new StandardXYItemRenderer();
            renderer.setBaseSeriesVisible(true, false);
            plot = new XYPlot(dataset, timeAxis, valueAxis, renderer);
        }

        if (hasYAxis2) {
            // second Y axis uses a separate dataset and axis
            plot.setDataset(1, new DataTupleXYDataset(stacked));

            valueAxis = new NumberAxis();
            valueAxis.setAutoRangeIncludesZero(true);

            plot.setRangeAxis(1, valueAxis);
            plot.mapDatasetToRangeAxis(1, 1);

            if (stacked) {
                // secondary axis data cannot be stacked, use a new renderer
                StandardXYItemRenderer renderer = new StandardXYItemRenderer();
                renderer.setBaseSeriesVisible(true, false);

                plot.setRenderer(1, renderer);
            }
            else {
                // reuse the existing rendering to ensure consistent appearance and continue color
                // cycling
                plot.setRenderer(1, plot.getRenderer());
                // TODO fix by creating rederer subclass that extends lookup series paint to
                // multiplex the series number
                // overwrite drawItem to set the series for paint to be series + plot1.seriesSize
            }
        }

        // null title font = it will be set in format
        // legend will be decided by callers
        return new JFreeChart("", null, plot, false);
    }

    protected void formatChart() {
        super.formatChart();

        XYPlot plot = chart.getXYPlot();

        if (stacked) {
            StackedXYAreaRenderer2 renderer = (StackedXYAreaRenderer2) plot.getRenderer();
            renderer.setLegendArea(new java.awt.Rectangle(10, 10));

            renderer.setBaseToolTipGenerator(tooltipGenerator);
        }
        else {
            // show filled markers at each data point
            StandardXYItemRenderer renderer = (StandardXYItemRenderer) plot.getRenderer();

            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(true);

            // if no data for more than 1 granularity's time period, do not draw a connecting line
            renderer.setPlotDiscontinuous(true);
            renderer.setGapThresholdType(UnitType.ABSOLUTE);

            recalculateGapThreshold(chart);

            renderer.setBaseToolTipGenerator(tooltipGenerator);
        }

        for (int i = 0; i < plot.getRangeAxisCount(); i++) {
            plot.getRangeAxis(i).setLabelFont(LABEL_FONT);
            plot.getRangeAxis(i).setTickLabelFont(AXIS_FONT);
        }

        plot.getDomainAxis().setLabelFont(LABEL_FONT);
        plot.getDomainAxis().setTickLabelFont(AXIS_FONT);

        // gray grid lines
        plot.setRangeGridlinePaint(GRID_COLOR);
        plot.setRangeGridlineStroke(GRID_LINES);
    }

    public void addLine(LineChartDefinition lineDefinition, DataSet data) {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        for (DataDefinition definition : lineDefinition.getLines()) {
            DataTupleXYDataset dataset = (DataTupleXYDataset) chart.getXYPlot().getDataset(
                    definition.usesSecondaryYAxis() ? 1 : 0);

            addMatchingData(dataset, definition, data, lineDefinition.getLineNamingMode());
        }

        updateChart();

    }

    public void addLinesForData(DataDefinition definition, DataSet data, NamingMode lineNamingMode) {
        DataTupleXYDataset dataset = (DataTupleXYDataset) chart.getXYPlot().getDataset(
                definition.usesSecondaryYAxis() ? 1 : 0);

        addMatchingData(dataset, definition, data, lineNamingMode);

        updateChart();
    }

    private void addMatchingData(DataTupleXYDataset dataset, DataDefinition definition, DataSet data,
            NamingMode lineNamingMode) {
        if (definition.matchesHost(data)) {
            for (DataType type : definition.getMatchingTypes(data)) {
                List<String> fields = definition.getMatchingFields(type);
                List<String> fieldNames = new java.util.ArrayList<String>(fields.size());

                for (String field : fields) {
                    fieldNames.add(lineNamingMode.getName(definition, data, type, field, granularity));
                }

                addData(dataset, data, type, fields, fieldNames);
            }
        }
    }

    private void addData(DataTupleXYDataset dataset, DataSet data, DataType type, List<String> fields,
            List<String> fieldNames) {
        double[] totals = new double[fields.size()];
        // use NaN as chart data when no values are defined rather than 0
        java.util.Arrays.fill(totals, Double.NaN);

        int n = 0;

        long lastOutputTime = Math.max(interval.getStart(), data.getStartTime());

        for (DataRecord record : data.getRecords(interval)) {
            if ((record != null) && record.hasData(type)) {
                for (int i = 0; i < fields.size(); i++) {
                    if (type.hasField(fields.get(i))) {
                        double value = record.getData(type, fields.get(i));

                        if (!Double.isNaN(value)) {
                            if (Double.isNaN(totals[i])) {
                                totals[i] = 0;
                            }

                            totals[i] += value;
                        }
                    }
                }

                ++n;
            }
            // else no data for this type at this time but may still need to output

            if ((n > 0) && ((record.getTime() - lastOutputTime) >= granularity)) {
                FixedMillisecond graphTime = new FixedMillisecond(record.getTime());

                for (int i = 0; i < fields.size(); i++) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(new java.util.Date(record.getTime()) + "\t" + type + "\t" + totals[i] + "\t"
                                + totals[i] / n + "\t" + n + "\t" + (record.getTime() - lastOutputTime));
                    }

                    if (!Double.isNaN(totals[i])) {
                        // if the plot is listening for dataset changes, it will fire an event for
                        // every data point
                        // this causes a huge amount of GC and very slow response times so the false
                        // value is important here
                        dataset.add(graphTime, totals[i] / n, fieldNames.get(i), false);
                    }

                    totals[i] = Double.NaN;
                }

                lastOutputTime = record.getTime();
                n = 0;
            }
        }

        // output final data point, if needed
        long endTime = data.getEndTime();

        if (endTime != lastOutputTime) {
            FixedMillisecond graphTime = new FixedMillisecond(endTime);

            for (int i = 0; i < fields.size(); i++) {
                if (logger.isTraceEnabled()) {
                    logger.trace(new java.util.Date(endTime) + "\t" + type + "\t" + totals[i] + "\t" + totals[i] / n
                            + "\t" + n + "\t" + (endTime - lastOutputTime));
                }

                if (!Double.isNaN(totals[i])) {
                    dataset.add(graphTime, totals[i] / n, fieldNames.get(i), false);
                }
            }
        }

        // fieldName may not have been used if there was no data
        // so, search the dataset first before associating tuples
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            int idx = fieldNames.indexOf(dataset.getSeriesKey(i));

            if (idx != -1) {
                dataset.associateTuple(fieldNames.get(idx), null, new DataTuple(data, type, fields.get(idx)));
            }
        }
    }

    private void updateChart() {
        recalculateGapThreshold(chart);

        chart.getXYPlot().getRangeAxis(0).configure();

        int seriesCount = chart.getXYPlot().getDataset(0).getSeriesCount();

        if (chart.getXYPlot().getDatasetCount() > 1) {
            seriesCount += chart.getXYPlot().getDataset(1).getSeriesCount();

            NumberAxis a = (NumberAxis) chart.getXYPlot().getRangeAxis(1);
            a.configure();
        }

        if ((seriesCount > 1) && (chart.getLegend() == null)) {
            addLegend();
        }
    }

    private void recalculateGapThreshold(JFreeChart chart) {
        // TODO handle stacked and 2 axis
        if (!stacked) {
            XYPlot plot = chart.getXYPlot();

            if (plot.getDataset().getItemCount(0) > 0) {
                DataTupleXYDataset dataset = (DataTupleXYDataset) plot.getDataset();
                int seriesCount = dataset.getSeriesCount();

                double[] averageDistance = new double[seriesCount];
                int[] count = new int[seriesCount];

                double[] previousX = new double[seriesCount];

                java.util.Arrays.fill(averageDistance, 0);
                java.util.Arrays.fill(count, 0);
                java.util.Arrays.fill(previousX, dataset.getXValue(0, 0));

                for (int i = 1; i < dataset.getItemCount(0); i++) {
                    double currentX = dataset.getXValue(0, i);

                    for (int j = 0; j < seriesCount; j++) {
                        double y = dataset.getYValue(j, i);

                        if (!Double.isNaN(y)) {
                            averageDistance[j] += currentX - previousX[j];
                            previousX[j] = currentX;
                            ++count[j];
                        }
                    }
                }

                double maxAverage = Double.MIN_VALUE;

                for (int i = 0; i < seriesCount; i++) {
                    averageDistance[i] /= count[i];

                    if (averageDistance[i] > maxAverage) {
                        maxAverage = averageDistance[i];
                    }
                }

                //((StandardXYItemRenderer) plot.getRenderer()).setGapThreshold(Integer.MAX_VALUE);
                ((StandardXYItemRenderer) plot.getRenderer()).setGapThreshold(maxAverage * 1.25);
            }
            else {
                ((StandardXYItemRenderer) plot.getRenderer()).setGapThreshold(Integer.MAX_VALUE);
            }
        }
    }

    /**
     * Sets the X axis to display time relative to the given start time.
     */
    // for relative time, format the x axis differently
    // the data _does not_ change
    public static void setRelativeAxis(JFreeChart chart, long startTime) {
        if (chart != null) {
            RelativeDateFormat format = new RelativeDateFormat(startTime);
            // : separators
            format.setHourSuffix(":");
            format.setMinuteSuffix(":");
            format.setSecondSuffix("");

            // zero pad minutes and seconds
            DecimalFormat padded = new DecimalFormat("00");
            format.setMinuteFormatter(padded);
            format.setSecondFormatter(padded);

            XYPlot plot = chart.getXYPlot();

            ((DateAxis) plot.getDomainAxis()).setDateFormatOverride(format);
        }
    }

    /**
     * Sets the X axis to display absolute time. This is the default.
     * 
     * @param chart
     */
    public static void setAbsoluteAxis(JFreeChart chart) {
        if (chart != null) {
            XYPlot plot = chart.getXYPlot();

            ((DateAxis) plot.getDomainAxis()).setDateFormatOverride(null);
        }
    }

    /**
     * This only sets the first Y axis as a percent. There is no support for having other axes with
     * percent scales.
     */
    public static void setPercentYAxis(JFreeChart chart) {
        NumberAxis yAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        yAxis.setRange(0, 100);
    }

    // customize tool tips on the graph to display the date time and the value
    private final XYToolTipGenerator tooltipGenerator = new XYToolTipGenerator() {
        private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
        private final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0.000");

        @Override
        public String generateToolTip(XYDataset dataset, int series, int item) {
            return (dataset.getSeriesCount() > 1 ? dataset.getSeriesKey(series) + " " : "")
                    + DATE_FORMAT.format(new java.util.Date((long) dataset.getXValue(series, item))) + " - "
                    + NUMBER_FORMAT.format(dataset.getYValue(series, item));
        }
    };
}
