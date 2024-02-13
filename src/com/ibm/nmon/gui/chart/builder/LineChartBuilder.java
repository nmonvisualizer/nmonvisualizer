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

import com.ibm.nmon.analysis.Statistic;

public class LineChartBuilder extends BaseChartBuilder<LineChartDefinition> {
    private boolean showLegends = true;

    public LineChartBuilder() {
        super();
    }

    protected JFreeChart createChart() {
        DateAxis timeAxis = new DateAxis();

        NumberAxis valueAxis = new NumberAxis();
        valueAxis.setAutoRangeIncludesZero(true);

        DataTupleXYDataset dataset = new DataTupleXYDataset(definition.isStacked());

        XYPlot plot = null;

        if (definition.isStacked()) {
            StackedXYAreaRenderer2 renderer = new StackedXYAreaRenderer2();
            renderer.setBaseSeriesVisible(true, false);

            plot = new XYPlot(dataset, timeAxis, valueAxis, renderer);
        }
        else {
            StandardXYItemRenderer renderer = new StandardXYItemRenderer();
            renderer.setBaseSeriesVisible(true, false);

            plot = new XYPlot(dataset, timeAxis, valueAxis, renderer);
        }

        if (definition.hasSecondaryYAxis()) {
            // second Y axis uses a separate dataset and axis
            plot.setDataset(1, new DataTupleXYDataset(definition.isStacked()));

            valueAxis = new NumberAxis();
            valueAxis.setAutoRangeIncludesZero(true);

            // secondary axis data cannot be stacked, so use the the standard, line based
            // rendering
            // for both types
            StandardXYItemRenderer renderer = new StandardXYItemRenderer();
            renderer.setBaseSeriesVisible(true, false);

            plot.setRangeAxis(1, valueAxis);
            plot.setRenderer(1, renderer);
            plot.mapDatasetToRangeAxis(1, 1);
        }

        // null title font = it will be set in format
        // legend will be decided by callers
        return new JFreeChart("", null, plot, false);
    }

    protected void formatChart() {
        super.formatChart();

        XYPlot plot = chart.getXYPlot();

        plot.getDomainAxis().setLabel(definition.getXAxisLabel());
        plot.getRangeAxis().setLabel(definition.getYAxisLabel());

        if (definition.usePercentYAxis()) {
            LineChartBuilder.setPercentYAxis(chart);
        }

        if (definition.isStacked()) {
            StackedXYAreaRenderer2 renderer = (StackedXYAreaRenderer2) plot.getRenderer();
            renderer.setLegendArea(new java.awt.Rectangle(10, 10));

            renderer.setBaseToolTipGenerator(tooltipGenerator);

            if (!definition.showDataPoints()) {
                renderer.setAutoPopulateSeriesShape(false);
                renderer.setBaseShape(new java.awt.Rectangle(), false);
            }
        }
        else {
            formatRenderer(0);
        }

        if (definition.hasSecondaryYAxis()) {
            plot.getRangeAxis(1).setLabel(definition.getSecondaryYAxisLabel());
            formatRenderer(1);
        }
    }

    private void formatRenderer(int index) {
        // show filled markers at each data point
        StandardXYItemRenderer renderer = (StandardXYItemRenderer) chart.getXYPlot().getRenderer(index);

        renderer.setBaseShapesVisible(true);
        renderer.setBaseShapesFilled(true);

        // if no data for more than 1 granularity's time period, do not draw a
        // connecting line
        renderer.setPlotDiscontinuous(true);
        renderer.setGapThresholdType(UnitType.ABSOLUTE);

        recalculateGapThreshold(index);

        renderer.setBaseToolTipGenerator(tooltipGenerator);

        if (!definition.showDataPoints()) {
            renderer.setAutoPopulateSeriesShape(false);
            renderer.setBaseShape(new java.awt.Rectangle(), false);
        }
    }

    public void addLine(DataSet data) {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        for (DataDefinition dataDefinition : definition.getData()) {
            DataTupleXYDataset dataset = (DataTupleXYDataset) chart.getXYPlot()
                    .getDataset(dataDefinition.usesSecondaryYAxis() ? 1 : 0);

            addMatchingData(dataset, dataDefinition, data, definition.getLineNamingMode());
        }

        updateChart();

    }

    public void addLinesForData(DataDefinition definition, DataSet data, NamingMode lineNamingMode) {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        DataTupleXYDataset dataset = (DataTupleXYDataset) chart.getXYPlot()
                .getDataset(definition.usesSecondaryYAxis() ? 1 : 0);

        addMatchingData(dataset, definition, data, lineNamingMode);

        updateChart();
    }

    private void addMatchingData(DataTupleXYDataset dataset, DataDefinition definition, DataSet data,
            NamingMode lineNamingMode) {

        if (definition == null) {
            throw new IllegalArgumentException("LineChartDefintion cannot be null");
        }

        if (definition.matchesHost(data)) {
            for (DataType type : definition.getMatchingTypes(data)) {
                List<String> fields = definition.getMatchingFields(type);
                List<String> fieldNames = new java.util.ArrayList<String>(fields.size());

                for (String field : fields) {
                    fieldNames.add(
                            lineNamingMode.getName(definition, data, type, field, getInterval(), getGranularity()));
                }

                addData(definition, dataset, data, type, fields, fieldNames);
            }
        }
    }

    private void addData(DataDefinition dataDefinition, DataTupleXYDataset dataset, DataSet data, DataType type,
            List<String> fields, List<String> fieldNames) {
        long start = System.nanoTime();

        double[] totals = new double[fields.size()];
        // use NaN as chart data when no values are defined rather than 0
        java.util.Arrays.fill(totals, Double.NaN);

        int n = 0;

        long lastOutputTime = Math.max(getInterval().getStart(), data.getStartTime());

        for (DataRecord record : data.getRecords(getInterval())) {
            if ((record != null) && record.hasData(type)) {
                for (int i = 0; i < fields.size(); i++) {
                    if (type.hasField(fields.get(i))) {
                        double value = record.getData(type, fields.get(i));

                        if (!Double.isNaN(value)) {
                            if (Double.isNaN(totals[i])) {
                                if (dataDefinition.getStatistic() == Statistic.MINIMUM) {
                                    totals[i] = Double.MAX_VALUE;
                                }
                                else {
                                    totals[i] = 0;
                                }
                            }

                            switch (dataDefinition.getStatistic()) {
                            case AVERAGE:
                                totals[i] += value;
                                break;
                            case MAXIMUM:
                                if (value > totals[i]) {
                                    totals[i] = value;
                                }
                                ;
                                break;
                            case MINIMUM:
                                if (value < totals[i]) {
                                    totals[i] = value;
                                }
                                ;
                                break;
                            case COUNT:
                                totals[i] += 1;
                                break;
                            case SUM:
                                ++totals[i];
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "canonot calculate " + dataDefinition.getStatistic() + " on a line chart");
                            }
                        }
                    }
                }

                ++n;
            }
            // else no data for this type at this time but may still need to output

            if ((n > 0) && ((record.getTime() - lastOutputTime) >= getGranularity())) {
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
                        if (dataDefinition.getStatistic() == Statistic.AVERAGE) {
                            dataset.add(graphTime, totals[i] / n, fieldNames.get(i), false);
                        }
                        else {
                            dataset.add(graphTime, totals[i], fieldNames.get(i), false);
                        }
                    }

                    // reset totals
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

        if (logger.isDebugEnabled()) {
            logger.debug("{}: {}-({} fields) added {} data points to chart '{}'  in {}ms", data, type,
                    fieldNames.size(), dataset.getItemCount(), definition.getTitle(),
                    (System.nanoTime() - start) / 1000000.0d);
        }
    }

    private void updateChart() {
        recalculateGapThreshold(0);

        if (definition.hasSecondaryYAxis()) {
            recalculateGapThreshold(1);
        }

        chart.getXYPlot().configureRangeAxes();

        if (chart.getLegend() == null) {
            int seriesCount = chart.getXYPlot().getDataset(0).getSeriesCount();

            if (definition.hasSecondaryYAxis()) {
                seriesCount += chart.getXYPlot().getDataset(1).getSeriesCount();
            }

            if ((seriesCount > 1) && showLegends) {
                addLegend();
            }
        }
    }

    private void recalculateGapThreshold(int datasetIndex) {
        if (definition.isStacked() && (datasetIndex == 0)) {
            return;
        }

        long start = System.nanoTime();

        XYPlot plot = chart.getXYPlot();
        DataTupleXYDataset dataset = (DataTupleXYDataset) plot.getDataset(datasetIndex);
        int seriesCount = dataset.getSeriesCount();

        // for each series, calculate the average distance between X values
        // use the maximum of those averages as the gap threshold
        double maxAverage = Double.MIN_VALUE;

        for (int i = 0; i < seriesCount; i++) {
            int itemCount = dataset.getItemCount();

            // no data; gaps are meaningless
            if (itemCount == 0) {
                continue;
            }

            // not all X values will have data; so count rather than using itemCount for the average
            int actualCount = 0;
            double averageDistance = 0;
            // do not start calculations until the first value is found
            double previousX = dataset.getXValue(i, 0);
            double previousY = dataset.getYValue(i, 0);

            for (int j = 1; j < itemCount; j++) {
                // find the first non-NaN Y value
                if (Double.isNaN(previousY)) {
                    previousX = dataset.getXValue(i, j);
                    previousY = dataset.getYValue(i, j);
                    continue;
                }

                // only calculate if there is a Y value at this X value
                double currentX = dataset.getXValue(i, j);
                double currentY = dataset.getYValue(i, j);

                // find the next non-NaN Y value
                if (Double.isNaN(currentY)) {
                    continue;
                }

                averageDistance += currentX - previousX;
                ++actualCount;

                previousX = currentX;
                previousY = currentY;
            }

            averageDistance /= actualCount;

            if (averageDistance > maxAverage) {
                maxAverage = averageDistance;
            }

            if (logger.isTraceEnabled()) {
                logger.trace("average gap distance is {} for chart '{}', series '{}'", averageDistance,
                        definition.getTitle(), dataset.getSeriesKey(i));
            }
        }

        // no data => no gaps
        if (maxAverage != Double.MIN_VALUE) {
            // use the max average for all series, plus some leeway, as the threshold
            ((StandardXYItemRenderer) plot.getRenderer(datasetIndex)).setGapThreshold(maxAverage * 1.25);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("gap threshold recalculated to {} for chart '{}', series {} in {}ms", maxAverage * 1.25,
                    definition.getTitle(), datasetIndex, (System.nanoTime() - start) / 1E6d);
        }
    }

    public void showLegends(boolean showLegends) {
        this.showLegends = showLegends;
    }

    public boolean getShowLegends() {
        return showLegends;
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

            if (plot.getDomainAxis() instanceof DateAxis) {
                ((DateAxis) plot.getDomainAxis()).setDateFormatOverride(null);
            }
        }
    }

    /**
     * This only sets the first Y axis as a percent. There is no support for having other axes with percent scales.
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
