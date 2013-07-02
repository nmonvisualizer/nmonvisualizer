package com.ibm.nmon.chart.definition;

/**
 * Chart definition that has one or two labeled Y axes. The primary axis can also be set to display
 * as a percentage. Y axis charts can also be 'stacked', where the values are somehow overlaid on
 * top of previous values. Depending on the implementation, stacking may not be allowed with
 * multiple axes.
 */
public abstract class YAxisChartDefinition extends BaseChartDefinition {
    private String yAxisLabel = "";
    private String secondaryYAxisLabel = "";

    private boolean percentYAxis = false;
    private boolean hasSecondaryYAxis = false;

    private final boolean stacked;

    protected YAxisChartDefinition(String shortName, String title, boolean stacked) {
        super(shortName, title);

        this.stacked = stacked;
    }

    public YAxisChartDefinition(YAxisChartDefinition copy, boolean copyData) {
        super(copy, copyData);

        this.stacked = copy.stacked;

        this.yAxisLabel = copy.yAxisLabel;
        this.secondaryYAxisLabel = copy.secondaryYAxisLabel;

        this.percentYAxis = copy.percentYAxis;
        this.hasSecondaryYAxis = copy.hasSecondaryYAxis;
    }

    public final boolean isStacked() {
        return stacked;
    }

    public final void setUsePercentYAxis(boolean percentYAxis) {
        this.percentYAxis = percentYAxis;
    }

    public final boolean usePercentYAxis() {
        return percentYAxis;
    }

    public final String getYAxisLabel() {
        return yAxisLabel;
    }

    public final void setYAxisLabel(String yAxisLabel) {
        if (yAxisLabel == null) {
            this.yAxisLabel = "";
        }
        else {
            this.yAxisLabel = yAxisLabel;
        }
    }

    public final String getSecondaryYAxisLabel() {
        return secondaryYAxisLabel;
    }

    public final void setSecondaryYAxisLabel(String secondaryYAxisLabel) {
        if (secondaryYAxisLabel == null) {
            this.secondaryYAxisLabel = "";
        }
        else {
            this.secondaryYAxisLabel = secondaryYAxisLabel;
        }
    }

    public final boolean hasSecondaryYAxis() {
        return hasSecondaryYAxis;
    }

    public final void setHasSecondaryYAxis(boolean secondaryYAxis) {
        this.hasSecondaryYAxis = secondaryYAxis;
    }
}
