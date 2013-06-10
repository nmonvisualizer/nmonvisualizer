package com.ibm.nmon.chart.definition;

import java.util.List;

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

    private final List<DataDefinition> lines;

    public LineChartDefinition(String shortName, String title) {
        this(shortName, title, true);
    }

    public LineChartDefinition(String shortName, String title, boolean stacked) {
        super(shortName, title, stacked);

        lineNamingMode = NamingMode.FIELD;
        lines = new java.util.ArrayList<DataDefinition>(3);
    }

    public final String getXAxisLabel() {
        return xAxisLabel;
    }

    public final void setXAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
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

    public final void addLine(DataDefinition line) {
        lines.add(line);
    }

    public final Iterable<DataDefinition> getLines() {
        return java.util.Collections.unmodifiableList(lines);
    }
}
