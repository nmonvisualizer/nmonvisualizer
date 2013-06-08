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

public final class BarChartBuilder extends BaseChartBuilder {
    private static final java.awt.Color OUTLINE_COLOR = new java.awt.Color(0xCCCCCC);

    private boolean stacked = false;

    public BarChartBuilder() {
        super();
    }

    public void setStacked(boolean stacked) {
        this.stacked = stacked;
    }

    public void initChart(BarChartDefinition definition) {
        setStacked(definition.isStacked());

        initChart();

        if (definition.usePercentYAxis()) {
            setPercentYAxis();
        }

        chart.setTitle(definition.getTitle());

        ((CategoryPlot) chart.getPlot()).getRangeAxis().setLabel(definition.getYAxisLabel());
        ((CategoryPlot) chart.getPlot()).getDomainAxis().setLabel(definition.getCategoryAxisLabel());
    }

    protected JFreeChart createChart() {
        CategoryAxis categoryAxis = new CategoryAxis();
        ValueAxis valueAxis = new NumberAxis();

        BarRenderer renderer = null;

        if (stacked) {
            renderer = new StackedBarRenderer();
        }
        else {
            renderer = new BarRenderer();
        }

        CategoryPlot plot = new CategoryPlot(new DataTupleCategoryDataset(false), categoryAxis, valueAxis, renderer);

        return new HighlightableBarChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    }

    @Override
    protected void formatChart() {
        super.formatChart();

        CategoryPlot plot = (CategoryPlot) chart.getPlot();

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter(new SimpleGradientBarPainter());
        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator("{1} {0} - {2} ({3})",
                Styles.NUMBER_FORMAT));

        renderer.setBaseOutlineStroke(new BasicStroke(3));
        renderer.setBaseOutlinePaint(OUTLINE_COLOR);

        plot.getDomainAxis().setCategoryMargin(0.15d);
        plot.getDomainAxis().setTickMarksVisible(false);

        // position of first bar start and last bar end
        // 1.5% of the chart area within the axis will be blank space on each end
        plot.getDomainAxis().setLowerMargin(.015);
        plot.getDomainAxis().setUpperMargin(.015);

        plot.getRangeAxis().setLabelFont(LABEL_FONT);
        plot.getRangeAxis().setTickLabelFont(AXIS_FONT);

        plot.getDomainAxis().setLabelFont(LABEL_FONT);
        plot.getDomainAxis().setTickLabelFont(AXIS_FONT);

        // gray grid lines
        plot.setRangeGridlinePaint(GRID_COLOR);
        plot.setRangeGridlineStroke(GRID_LINES);
    }

    public void addBar(BarChartDefinition barDefinition, AnalysisRecord record) {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        if (barDefinition == null) {
            throw new IllegalArgumentException("BarChartDefintion cannot be null");
        }

        // Note that both this builder and the AnalysisRecord have an Interval stored. Note that
        // these are _not_ synchronized here under the assumption that a) the client application has
        // already done this and b) the client application is caching a number of records and does
        // not expect different records to have different Intervals. So, the record's internal
        // Interval is used rather than this class' record.

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        DataTupleCategoryDataset dataset = (DataTupleCategoryDataset) plot.getDataset();
        DataSet data = record.getDataSet();
        Statistic previousStat = null;

        for (DataDefinition definition : barDefinition.getCategories()) {
            if (definition.matchesHost(data)) {
                for (DataType type : definition.getMatchingTypes(data)) {
                    for (String field : definition.getMatchingFields(type)) {
                        String barName = barDefinition.getBarNamingMode().getName(definition, data, type, field,
                                granularity);
                        String categoryName = barDefinition.getCategoryNamingMode().getName(definition, data, type,
                                field, granularity);

                        Statistic currentStat = definition.getStatistic();
                        double value = currentStat.getValue(record, type, field);

                        if ((previousStat != null) && (previousStat != currentStat)) {
                            dataset.setCategoriesHaveDifferentStats(true);
                        }

                        previousStat = currentStat;

                        dataset.addValue(value, barName, categoryName);
                        dataset.associateTuple(barName, categoryName, new DataTuple(data, type, field));
                    }
                }
            }
        }

        if ((dataset.getRowCount() > 1) && (chart.getLegend() == null)) {
            addLegend();
        }

        // subtract the value for each category from the previous values
        if (barDefinition.isSubtractionNeeded() && (dataset.getRowCount() != 0)) {
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
    }

    public void setPercentYAxis() {
        NumberAxis yAxis = (NumberAxis) ((CategoryPlot) chart.getPlot()).getRangeAxis();
        yAxis.setRange(0, 100);
    }
}
