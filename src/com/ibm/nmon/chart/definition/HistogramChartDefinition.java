package com.ibm.nmon.chart.definition;

import java.util.List;

import com.ibm.nmon.data.definition.NamingMode;

import com.ibm.nmon.analysis.Statistic;

public final class HistogramChartDefinition extends YAxisChartDefinition {
    private int bins = 10;

    private String xAxisLabel = "";

    private NamingMode histogramNamingMode;

    private final List<Statistic> markers;

    public HistogramChartDefinition(String shortName, String title) {
        super(shortName, title, false);

        histogramNamingMode = NamingMode.FIELD;

        markers = new java.util.ArrayList<Statistic>(3);
        markers.add(Statistic.AVERAGE);
        markers.add(Statistic.MEDIAN);
        markers.add(Statistic.PERCENTILE_95);
    }

    public HistogramChartDefinition(HistogramChartDefinition copy, boolean copyData) {
        super(copy, copyData);

        this.bins = copy.bins;
        this.xAxisLabel = copy.xAxisLabel;
        this.histogramNamingMode = copy.histogramNamingMode;
        this.markers = new java.util.ArrayList<Statistic>(copy.markers);
    }

    public int getBins() {
        return bins;
    }

    public void setBins(int bins) {
        this.bins = bins;
    }

    public String getXAxisLabel() {
        return xAxisLabel;
    }

    public final void setXAxisLabel(String xAxisLabel) {
        if (xAxisLabel == null) {
            this.xAxisLabel = "";
        }
        else {
            this.xAxisLabel = xAxisLabel;
        }
    }

    public NamingMode getHistogramNamingMode() {
        return histogramNamingMode;
    }

    public void setHistogramNamingMode(NamingMode histogramNamingMode) {
        if (histogramNamingMode == null) {
            this.histogramNamingMode = NamingMode.FIELD;
        }
        else {
            this.histogramNamingMode = histogramNamingMode;
        }
    }

    public int getMarkerCount() {
        return markers.size();
    }

    public Iterable<Statistic> getMarkers() {
        return java.util.Collections.unmodifiableList(markers);
    }

    public void setMarkers(Statistic... stats) {
        markers.clear();

        for (Statistic stat : stats) {
            markers.add(stat);
        }
    }
}
