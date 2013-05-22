package com.ibm.nmon.chart.definition;

/**
 * Chart definition that has a labeled Y axis. The axis can also be set to display as a percentage.
 * Y axis charts can also be 'stacked', where the values are somehow overlaid on top of previous
 * values.
 */
public abstract class YAxisChartDefinition extends BaseChartDefinition {
    private String yAxisLabel = "";

    private boolean percentYAxis = false;

    private final boolean stacked;

    protected YAxisChartDefinition(String shortName, String title, boolean stacked) {
        super(shortName, title);

        this.stacked = stacked;
    }

    public boolean isStacked() {
        return stacked;
    }

    public void setUsePercentYAxis(boolean percentYAxis) {
        this.percentYAxis = percentYAxis;
    }

    public boolean usePercentYAxis() {
        return percentYAxis;
    }

    public String getYAxisLabel() {
        return yAxisLabel;
    }

    public void setYAxisLabel(String yAxisLabel) {
        if (yAxisLabel == null) {
            this.yAxisLabel = "";
        }
        else {
            this.yAxisLabel = yAxisLabel;
        }
    }
}
