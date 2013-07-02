package com.ibm.nmon.chart.definition;

import java.util.List;

import com.ibm.nmon.data.definition.DataDefinition;

/**
 * Base class for chart definitions. A chart definition will have a short name and a title. The
 * short name is used as an identifier in the GUI. The title will be used as the title in the chart.
 */
public abstract class BaseChartDefinition {
    private final String shortName;
    private final String title;

    private final List<DataDefinition> data;

    protected BaseChartDefinition(String shortName, String title) {
        if (shortName == null) {
            this.shortName = "";
        }
        else {
            this.shortName = shortName;
        }

        if (title == null) {
            this.title = "";
        }
        else {
            this.title = title;
        }

        data = new java.util.ArrayList<DataDefinition>(3);
    }

    public final String getShortName() {
        return shortName;
    }

    public final String getTitle() {
        return title;
    }

    public final void addData(DataDefinition dataDefinition) {
        data.add(dataDefinition);
    }

    public final Iterable<DataDefinition> getData() {
        return java.util.Collections.unmodifiableList(data);
    }
}
