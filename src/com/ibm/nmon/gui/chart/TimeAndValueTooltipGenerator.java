package com.ibm.nmon.gui.chart;

import org.jfree.chart.labels.XYToolTipGenerator;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.jfree.data.xy.XYDataset;

public final class TimeAndValueTooltipGenerator implements XYToolTipGenerator {
    private final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0.000");

    public TimeAndValueTooltipGenerator() {}

    @Override
    public String generateToolTip(XYDataset dataset, int series, int item) {
        return (dataset.getSeriesCount() > 1 ? dataset.getSeriesKey(series) + " " : "")
                + TIME_FORMAT.format(new java.util.Date((long) dataset.getXValue(series, item))) + " - "
                + NUMBER_FORMAT.format(dataset.getYValue(series, item));
    }

    public void setTimeZone(TimeZone timeZone) {
        TIME_FORMAT.setTimeZone(timeZone);
    }
}
