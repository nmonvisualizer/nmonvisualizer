package com.ibm.nmon.gui.chart.builder;

import java.awt.BasicStroke;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;

import org.jfree.chart.plot.CategoryPlot;

import org.jfree.chart.labels.StandardCategoryToolTipGenerator;

import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;

import com.ibm.nmon.analysis.AnalysisRecord;
import com.ibm.nmon.analysis.Statistic;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataTuple;

import com.ibm.nmon.data.definition.DataDefinition;
import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.chart.HighlightableBarChart;
import com.ibm.nmon.gui.chart.data.DataTupleCategoryDataset;

import com.ibm.nmon.chart.definition.BarChartDefinition;

public final class BarChartBuilder extends BaseChartBuilder<BarChartDefinition> {
    private static final java.awt.Color OUTLINE_COLOR = new java.awt.Color(0xCCCCCC);

    public BarChartBuilder() {
        super();
    }

    protected JFreeChart createChart() {
        CategoryAxis categoryAxis = new CategoryAxis();
        ValueAxis valueAxis = new NumberAxis();

        BarRenderer renderer = null;

        if (definition.isStacked()) {
            renderer = new StackedBarRenderer();
        }
        else {
            renderer = new BarRenderer();
        }

        CategoryPlot plot = new CategoryPlot(new DataTupleCategoryDataset(false), categoryAxis, valueAxis, renderer);

        if (definition.hasSecondaryYAxis()) {
            // second Y axis uses a separate dataset and axis
            plot.setDataset(1, new DataTupleCategoryDataset(definition.isStacked()));

            valueAxis = new NumberAxis();

            plot.setRenderer(1, new BarRenderer());
            plot.setRangeAxis(1, valueAxis);
            plot.mapDatasetToRangeAxis(1, 1);
        }

        return new HighlightableBarChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    }

    @Override
    protected void formatChart() {
        super.formatChart();

        chart.setTitle(definition.getTitle());

        CategoryPlot plot = (CategoryPlot) chart.getPlot();

        plot.getDomainAxis().setLabel(definition.getCategoryAxisLabel());
        plot.getRangeAxis().setLabel(definition.getYAxisLabel());

        if (definition.hasSecondaryYAxis()) {
            plot.getRangeAxis(1).setLabel(definition.getSecondaryYAxisLabel());
        }

        if (definition.usePercentYAxis()) {
            setPercentYAxis();
        }

        for (int i = 0; i < plot.getRendererCount(); i++) {
            BarRenderer renderer = (BarRenderer) plot.getRenderer(i);

            renderer.setShadowVisible(false);
            renderer.setDrawBarOutline(false);
            renderer.setBarPainter(new GradientPainters.GradientBarPainter());

            renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator("{1} {0} - {2} ({3})",
                    Styles.NUMBER_FORMAT));

            renderer.setBaseOutlineStroke(new BasicStroke(3));
            renderer.setBaseOutlinePaint(OUTLINE_COLOR);

            plot.getRangeAxis(i).setLabelFont(LABEL_FONT);
            plot.getRangeAxis(i).setTickLabelFont(AXIS_FONT);
        }

        plot.getDomainAxis().setCategoryMargin(0.15d);
        plot.getDomainAxis().setTickMarksVisible(false);

        // position of first bar start and last bar end
        // 1.5% of the chart area within the axis will be blank space on each end
        plot.getDomainAxis().setLowerMargin(.015);
        plot.getDomainAxis().setUpperMargin(.015);

        plot.getDomainAxis().setLabelFont(LABEL_FONT);
        plot.getDomainAxis().setTickLabelFont(AXIS_FONT);

        // gray grid lines
        plot.setRangeGridlinePaint(GRID_COLOR);
        plot.setRangeGridlineStroke(GRID_LINES);
    }

    public void addBar(AnalysisRecord record) {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        if (definition == null) {
            throw new IllegalArgumentException("BarChartDefintion cannot be null");
        }

        // Note that both this builder and the AnalysisRecord have an Interval stored. Note that
        // these are _not_ synchronized here under the assumption that a) the client application has
        // already done this and b) the client application is caching a number of records and does
        // not expect different records to have different Intervals. So, the record's internal
        // Interval is used rather than this class' record.

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        DataTupleCategoryDataset dataset = (DataTupleCategoryDataset) plot
                .getDataset(definition.hasSecondaryYAxis() ? 1 : 0);
        DataSet data = record.getDataSet();
        Statistic previousStat = null;

        for (DataDefinition dataDefinition : definition.getData()) {
            if (dataDefinition.matchesHost(data)) {
                for (DataType type : dataDefinition.getMatchingTypes(data)) {
                    for (String field : dataDefinition.getMatchingFields(type)) {
                        String barName = definition.getBarNamingMode().getName(dataDefinition, data, type, field,
                                granularity);
                        String categoryName = definition.getCategoryNamingMode().getName(dataDefinition, data, type,
                                field, granularity);

                        Statistic currentStat = dataDefinition.getStatistic();
                        double value = currentStat.getValue(record, type, field);

                        if ((previousStat != null) && (previousStat != currentStat)) {
                            dataset.setCategoriesHaveDifferentStats(true);
                        }

                        previousStat = currentStat;

                        dataset.addValue(value, barName, categoryName);
                        dataset.associateTuple(barName, categoryName, new DataTuple(data, type, field));
                        addDataUsed(data);
                    }
                }
            }
        }

        // subtract the value for each category from the previous values
        if (definition.isSubtractionNeeded() && (dataset.getRowCount() != 0)) {
            for (int i = 0; i < dataset.getColumnCount(); i++) {
                // prime total with the first value in each bar
                // the first value is not modified
                double total = (double) dataset.getValue(0, i).doubleValue();
                String barName = (String) dataset.getColumnKey(i);

                for (int j = 1; j < dataset.getRowCount(); j++) {
                    double value = dataset.getValue(j, i).doubleValue() - total;

                    String categoryName = (String) dataset.getRowKey(j);
                    dataset.setValue(value, categoryName, barName);
                    total += value;
                }
            }
        }

        if (chart.getLegend() == null) {
            int rowCount = plot.getDataset(0).getRowCount();

            if (definition.hasSecondaryYAxis()) {
                rowCount += plot.getDataset(1).getRowCount();
            }

            if (rowCount > 1) {
                addLegend();
            }
        }

        plot.configureRangeAxes();
    }

    public void setPercentYAxis() {
        NumberAxis yAxis = (NumberAxis) ((CategoryPlot) chart.getPlot()).getRangeAxis();
        yAxis.setRange(0, 100);
    }
}
