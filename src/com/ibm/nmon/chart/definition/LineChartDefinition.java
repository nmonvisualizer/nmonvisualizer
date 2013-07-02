package com.ibm.nmon.chart.definition;

import com.ibm.nmon.data.definition.DataDefinition;
import com.ibm.nmon.data.definition.NamingMode;

/**
 * <p>
 * Defines a line chart which has a labeled X axis. Lines are defined as a collection of
 * {@link DataDefinition DataDefinitions}. Lines are named by a single {@link NamingMode}.
 * </p>
 */
public class LineChartDefinition extends YAxisChartDefinition {
    private String xAxisLabel = "";

    private NamingMode lineNamingMode;

    public LineChartDefinition(String shortName, String title) {
        this(shortName, title, false);
    }

    public LineChartDefinition(String shortName, String title, boolean stacked) {
        super(shortName, title, stacked);

        lineNamingMode = NamingMode.FIELD;
    }

    public final String getXAxisLabel() {
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

    public final NamingMode getLineNamingMode() {
        return lineNamingMode;
    }

    public final void setLineNamingMode(NamingMode mode) {
        if (mode == null) {
            lineNamingMode = NamingMode.FIELD;
        }
        else {
            lineNamingMode = mode;
        }
    }
}
