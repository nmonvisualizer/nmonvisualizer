package com.ibm.nmon.gui.chart.builder;

import java.text.DecimalFormat;

import java.util.List;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;

import org.jfree.chart.labels.StandardCategoryToolTipGenerator;

import org.jfree.chart.renderer.category.LineAndShapeRenderer;

import com.ibm.nmon.analysis.AnalysisRecord;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataTuple;

import com.ibm.nmon.data.definition.DataDefinition;

import com.ibm.nmon.chart.definition.IntervalChartDefinition;

import com.ibm.nmon.gui.chart.data.DataTupleCategoryDataset;
import com.ibm.nmon.util.TimeFormatCache;

public final class IntervalChartBuilder extends BaseChartBuilder<IntervalChartDefinition> {
    public IntervalChartBuilder() {
        super();
    }

    @Override
    protected JFreeChart createChart() {
        // stacked is always false for IntervalChart

        // note that IntervalChartDefinition is a line chart but this class creates a JFreeChart
        // category plot
        // interval charts use a non-numeric x-axis (1 value per interval) which requires a category
        // axis and plot; the chart will still draw a line however via the LineAndShapeRenderer
        CategoryAxis categoryAxis = new CategoryAxis();

        NumberAxis valueAxis = new NumberAxis();
        valueAxis.setAutoRangeIncludesZero(true);

        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setBaseSeriesVisible(true, false);

        CategoryPlot plot = new CategoryPlot(new DataTupleCategoryDataset(true), categoryAxis, valueAxis, renderer);

        if (definition.hasSecondaryYAxis()) {
            // second Y axis uses a separate dataset and axis
            plot.setDataset(1, new DataTupleCategoryDataset(false));

            valueAxis = new NumberAxis();
            valueAxis.setAutoRangeIncludesZero(true);

            renderer = new LineAndShapeRenderer();
            renderer.setBaseSeriesVisible(true, false);

            plot.setRenderer(1, renderer);
            plot.setRangeAxis(1, valueAxis);
            plot.mapDatasetToRangeAxis(1, 1);
        }

        // null title font => it will be set in format
        // legend will be decided by callers
        return new JFreeChart("", null, plot, false);
    }

    @Override
    protected void formatChart() {
        super.formatChart();

        chart.setTitle(definition.getTitle());

        CategoryPlot plot = (CategoryPlot) chart.getPlot();

        plot.getDomainAxis().setLabel(definition.getXAxisLabel());

        plot.getRangeAxis().setLabel(definition.getYAxisLabel());

        if (definition.usePercentYAxis()) {
            setPercentYAxis();
        }

        for (int i = 0; i < plot.getRendererCount(); i++) {
            LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer(i);

            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(true);

            renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator("{1} {0} - {2}", new DecimalFormat(
                    "#,##0.00")));

            plot.getRangeAxis(i).setLabelFont(LABEL_FONT);
            plot.getRangeAxis(i).setTickLabelFont(AXIS_FONT);
        }

        // position of first line start and last line end
        // 1.5% of the chart area within the axis will be blank space on each end
        plot.getDomainAxis().setLowerMargin(.015);
        plot.getDomainAxis().setUpperMargin(.015);

        plot.getDomainAxis().setLabelFont(LABEL_FONT);
        plot.getDomainAxis().setTickLabelFont(AXIS_FONT);

        // position of first point start and last line end
        // 1.5% of the chart area within the axis will be blank space on each end
        plot.getDomainAxis().setLowerMargin(.015);
        plot.getDomainAxis().setUpperMargin(.015);

        // let each interval name have as much room as possible
        plot.getDomainAxis().setCategoryMargin(0);

        // gray grid lines
        plot.setRangeGridlinePaint(GRID_COLOR);
        plot.setRangeGridlineStroke(GRID_LINES);
    }

    public void addLine(IntervalChartDefinition lineDefinition, List<AnalysisRecord> records) {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        if ((records == null) || records.isEmpty()) {
            // => no intervals defined
            return;
        }

        if (records.size() > 4) {
            ((CategoryPlot) chart.getPlot()).getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }

        DataTupleCategoryDataset dataset = (DataTupleCategoryDataset) ((CategoryPlot) chart.getPlot())
                .getDataset(lineDefinition.hasSecondaryYAxis() ? 1 : 0);
        // assume all records are from the same DataSet
        DataSet data = records.get(0).getDataSet();

        Map<String, Integer> usedIntervalNames = new java.util.HashMap<String, Integer>(records.size());

        for (DataDefinition definition : lineDefinition.getData()) {
            if (definition.matchesHost(data)) {
                for (DataType type : definition.getMatchingTypes(data)) {
                    List<String> fields = definition.getMatchingFields(type);

                    for (String field : fields) {
                        String name = lineDefinition.getLineNamingMode().getName(definition, data, type, field,
                                granularity);

                        for (AnalysisRecord record : records) {
                            double value = definition.getStatistic().getValue(record, type, field);

                            String intervalName = record.getInterval().getName();

                            if ("".equals(intervalName)) {
                                if (record.getInterval().getDuration() >= (86400 * 1000)) {
                                    intervalName = TimeFormatCache.formatDateTime(record.getInterval().getStart())
                                            + '-' + TimeFormatCache.formatDateTime(record.getInterval().getEnd());
                                }
                                else {
                                    intervalName = TimeFormatCache.formatTime(record.getInterval().getStart()) + '-'
                                            + TimeFormatCache.formatTime(record.getInterval().getEnd());
                                }
                            }

                            // prevent duplicate interval / data name combinations
                            Integer count = usedIntervalNames.get(intervalName);

                            if (count != null) {
                                int i = dataset.getColumnIndex(name);

                                if (i != -1) {
                                    usedIntervalNames.put(intervalName, ++count);
                                    intervalName += " " + count;
                                }
                            }
                            else {
                                usedIntervalNames.put(intervalName, 0);
                            }

                            dataset.addValue(value, name, intervalName);
                            dataset.associateTuple(name, intervalName, new DataTuple(record.getDataSet(), type, field));
                            addDataUsed(data);
                        }
                    }
                }
            }
        }

        if (chart.getLegend() == null) {
            int rowCount = chart.getCategoryPlot().getDataset(0).getRowCount();

            if (definition.hasSecondaryYAxis()) {
                rowCount += chart.getCategoryPlot().getDataset(1).getRowCount();
            }

            if (rowCount > 1) {
                addLegend();
            }
        }
    }

    public void setPercentYAxis() {
        NumberAxis yAxis = (NumberAxis) ((CategoryPlot) chart.getPlot()).getRangeAxis();
        yAxis.setRange(0, 100);
    }
}
