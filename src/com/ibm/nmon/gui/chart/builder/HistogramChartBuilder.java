package com.ibm.nmon.gui.chart.builder;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

import org.jfree.chart.renderer.xy.XYBarRenderer;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.DataTuple;

import com.ibm.nmon.data.definition.DataDefinition;
import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.analysis.AnalysisRecord;
import com.ibm.nmon.analysis.Statistic;

import com.ibm.nmon.gui.chart.data.DataTupleHistogramDataset;

import com.ibm.nmon.chart.definition.HistogramChartDefinition;

public final class HistogramChartBuilder extends BaseChartBuilder<HistogramChartDefinition> {
    private static final java.awt.Color OUTLINE_COLOR = new java.awt.Color(0xCCCCCC);
    private static final Font MARKER_FONT = new Font("null", Font.PLAIN, 12);
    private static final Color MARKER_COLOR = new Color(0x222266);
    private static final BasicStroke MARKER_LINE = new BasicStroke(.6f, 0, 0, 1.0f, new float[] { 1, 2, 5, 2 }, 5);

    // current vertical position for annotations
    // incremented in addHistogram to prevent overlapping
    private int insetTop = 5;

    public HistogramChartBuilder() {
        super();
    }

    @Override
    protected JFreeChart createChart() {
        HistogramDataset dataset = new DataTupleHistogramDataset();

        NumberAxis dataAxis = new NumberAxis();
        dataAxis.setAutoRangeIncludesZero(false);

        NumberAxis valueAxis = new NumberAxis();
        valueAxis.setAutoRangeIncludesZero(true);

        XYBarRenderer renderer = new XYBarRenderer();

        XYPlot plot = new XYPlot(dataset, dataAxis, valueAxis, renderer);

        if (definition.hasSecondaryYAxis()) {
            plot.setDataset(1, new DataTupleHistogramDataset());

            valueAxis = new NumberAxis();
            valueAxis.setAutoRangeIncludesZero(true);

            plot.setRangeAxis(1, valueAxis);
            plot.setRenderer(1, renderer);
            plot.mapDatasetToRangeAxis(1, 1);
        }

        insetTop = 5;

        return new JFreeChart("", null, plot, false);
    }

    protected void formatChart() {
        super.formatChart();

        chart.setTitle(definition.getTitle());

        XYPlot plot = chart.getXYPlot();

        if ("".equals(definition.getXAxisLabel())) {
            plot.getDomainAxis().setLabel("Values");
        }
        else {
            plot.getDomainAxis().setLabel(definition.getXAxisLabel());
        }

        if ("".equals(definition.getYAxisLabel())) {
            if (definition.usePercentYAxis()) {
                plot.getRangeAxis().setLabel("Percent");
            }
            else {
                plot.getRangeAxis().setLabel("Count");
            }
        }
        else {
            plot.getRangeAxis().setLabel(definition.getYAxisLabel());
        }

        HistogramDataset dataset = (HistogramDataset) plot.getDataset();

        if (definition.usePercentYAxis()) {
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setRange(0, 100);

            dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        }
        else {
            dataset.setType(HistogramType.FREQUENCY);
        }

        if (definition.hasSecondaryYAxis()) {
            // secondary axis always uses given label, even if blank
            plot.getRangeAxis(1).setLabel(definition.getSecondaryYAxisLabel());

            dataset = (HistogramDataset) plot.getDataset(1);
            // secondary axis cannot use relative frequency
            dataset.setType(HistogramType.FREQUENCY);
        }

        for (int i = 0; i < plot.getRendererCount(); i++) {
            XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer(i);

            renderer = ((XYBarRenderer) plot.getRenderer());
            renderer.setMargin(0.15d);

            renderer.setShadowVisible(false);
            renderer.setDrawBarOutline(false);
            renderer.setBarPainter(new GradientPainters.GradientXYBarPainter());

            renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{1} - {2}", Styles.NUMBER_FORMAT,
                    Styles.NUMBER_FORMAT));

            renderer.setBaseOutlineStroke(new BasicStroke(3));
            renderer.setBaseOutlinePaint(OUTLINE_COLOR);
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

    public void addHistogram(AnalysisRecord analysis) {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        XYPlot plot = chart.getXYPlot();
        DataTupleHistogramDataset dataset = (DataTupleHistogramDataset) plot
                .getDataset(definition.hasSecondaryYAxis() ? 1 : 0);
        DataSet data = analysis.getDataSet();

        for (DataDefinition dataDefinition : definition.getData()) {
            if (dataDefinition.matchesHost(data)) {
                for (DataType type : dataDefinition.getMatchingTypes(data)) {
                    for (String field : dataDefinition.getMatchingFields(type)) {
                        String fieldName = definition.getHistogramNamingMode().getName(dataDefinition, data, type,
                                field, granularity);

                        double[] values = new double[data.getRecordCount()];
                        int x = 0;

                        for (DataRecord record : data.getRecords()) {
                            values[x] = record.getData(type)[type.getFieldIndex(field)];
                            ++x;
                        }

                        dataset.addSeries(fieldName, values, definition.getBins());
                        dataset.associateTuple(fieldName, null, new DataTuple(data, type, fieldName));
                    }
                }

            }
        }

        // add markers if necessary
        if ((definition.getMarkerCount() > 0) && (dataset.getSeriesCount() > 0)) {
            int seriesCount = dataset.getSeriesCount();
            // output the field name in the marker if there is more than one series
            boolean useFieldName = seriesCount > 1;

            for (int i = 0; i < seriesCount; i++) {
                DataTuple tuple = dataset.getTuple(i, 0);

                for (Statistic stat : definition.getMarkers()) {
                    double value = stat.getValue(analysis, tuple.getDataType(), tuple.getField());

                    ValueMarker marker = new ValueMarker(value);
                    marker.setLabel((useFieldName ? dataset.getSeriesKey(i) + " " : "")
                            + stat.getName(getGranularity()) + ": " + Styles.NUMBER_FORMAT.format(value));
                    marker.setStroke(MARKER_LINE);
                    marker.setPaint(MARKER_COLOR);
                    marker.setLabelFont(MARKER_FONT);
                    marker.setLabelPaint(MARKER_COLOR);
                    marker.setLabelOffset(new RectangleInsets(insetTop, 5, 5, 10));
                    marker.setLabelTextAnchor(TextAnchor.TOP_CENTER);

                    plot.addDomainMarker(marker);

                    insetTop += 20;
                }
            }
        }

        chart.getXYPlot().configureRangeAxes();

        if (chart.getLegend() == null) {
            int seriesCount = chart.getXYPlot().getDataset(0).getSeriesCount();

            if (definition.hasSecondaryYAxis()) {
                seriesCount += chart.getXYPlot().getDataset(1).getSeriesCount();
            }

            if (seriesCount > 1) {
                addLegend();
            }
        }
    }
}
