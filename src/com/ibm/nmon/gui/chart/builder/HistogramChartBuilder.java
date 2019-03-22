package com.ibm.nmon.gui.chart.builder;

import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

import org.jfree.chart.renderer.xy.XYBarRenderer;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

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

        XYPlot plot = chart.getXYPlot();

        if ("".equals(definition.getXAxisLabel())) {
            plot.getDomainAxis().setLabel("Values");
        }
        else {
            plot.getDomainAxis().setLabel(definition.getXAxisLabel());
        }

        if (definition.getXAxisRange() != null) {
            plot.getDomainAxis().setRange(definition.getXAxisRange());
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

            formatter.formatRenderer(renderer);

            renderer.setBaseToolTipGenerator(
                    new StandardXYToolTipGenerator("{1} - {2}", Styles.NUMBER_FORMAT, Styles.NUMBER_FORMAT));
        }
    }

    public void addHistogram(AnalysisRecord analysis) {
        if (chart == null) {
            throw new IllegalStateException("initChart() must be called first");
        }

        if (definition == null) {
            throw new IllegalArgumentException("HistogramChartDefintion cannot be null");
        }

        long start = System.nanoTime();

        XYPlot plot = chart.getXYPlot();
        DataTupleHistogramDataset dataset = (DataTupleHistogramDataset) plot
                .getDataset(definition.hasSecondaryYAxis() ? 1 : 0);
        DataSet data = analysis.getDataSet();

        for (DataDefinition dataDefinition : definition.getData()) {
            if (dataDefinition.matchesHost(data)) {
                for (DataType type : dataDefinition.getMatchingTypes(data)) {
                    for (String field : dataDefinition.getMatchingFields(type)) {
                        String fieldName = definition.getHistogramNamingMode().getName(dataDefinition, data, type,
                                field, getInterval(), getGranularity());

                        List<Double> values = new java.util.ArrayList<Double>(data.getRecordCount());

                        // Use the analysis record's internal Interval is used rather than this
                        // class' record. Assume this class' interval and the record's are
                        // synchronized by the caller.

                        for (DataRecord record : data.getRecords(analysis.getInterval())) {
                            if (record.hasData(type)) {
                                values.add(record.getData(type)[type.getFieldIndex(field)]);
                            }
                        }

                        // convert to double for HistogramDataset
                        double[] toAdd = new double[values.size()];

                        for (int i = 0; i < toAdd.length; i++) {
                            toAdd[i] = values.get(i);
                        }

                        if (definition.getXAxisRange() == null) {
                            dataset.addSeries(fieldName, toAdd, definition.getBins());
                        }
                        else {
                            dataset.addSeries(fieldName, toAdd, definition.getBins(),
                                    definition.getXAxisRange().getLowerBound(),
                                    definition.getXAxisRange().getUpperBound());
                        }

                        dataset.associateTuple(fieldName, null, new DataTuple(data, type, field));

                        if (logger.isDebugEnabled()) {
                            logger.debug("{}: {}-{} added {} data points to chart '{}'  in {}ms", data, type, field,
                                    toAdd.length, definition.getTitle(), (System.nanoTime() - start) / 1000000.0d);
                        }
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
                    marker.setLabel((useFieldName ? dataset.getSeriesKey(i) + " " : "") + stat.getName(getGranularity())
                            + ": " + Styles.NUMBER_FORMAT.format(value));
                    formatter.formatMarker(marker, false, insetTop);

                    plot.addDomainMarker(marker);

                    insetTop += 20;
                }
            }
        }

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
