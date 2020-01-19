package com.ibm.nmon.chart.definition;

import java.util.List;

import com.ibm.nmon.data.definition.DataDefinition;
import com.ibm.nmon.data.definition.NamingMode;

/**
 * Base class for chart definitions. A chart definition will have a short name and a title. The short name is used as an
 * identifier in the GUI. The title will be used as the title in the chart.
 */
public abstract class BaseChartDefinition implements Cloneable {
    private String shortName;
    private String title;

    private int width;
    private int height;

    private final List<DataDefinition> data;

    private NamingMode subtitleNamingMode = NamingMode.HOST;

    protected BaseChartDefinition(String shortName, String title) {
        setShortName(shortName);
        setTitle(title);

        this.width = 1920;
        this.height = 1080;

        data = new java.util.ArrayList<DataDefinition>(3);
    }

    protected BaseChartDefinition(BaseChartDefinition copy, boolean copyData) {
        this.shortName = copy.shortName;
        this.title = copy.title;

        this.width = copy.width;
        this.height = copy.height;

        this.subtitleNamingMode = copy.subtitleNamingMode;

        if (copyData) {
            this.data = new java.util.ArrayList<DataDefinition>(copy.data);
        }
        else {
            this.data = new java.util.ArrayList<DataDefinition>(3);
        }
    }

    public final String getShortName() {
        return shortName;
    }

    public final void setShortName(String shortName) {
        if (shortName == null) {
            this.shortName = "";
        }
        else {
            this.shortName = shortName;
        }
    }

    public final String getTitle() {
        return title;
    }

    public final void setTitle(String title) {
        if (title == null) {
            this.title = "";
        }
        else {
            this.title = title;
        }
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        if (height < 1) {
            throw new IllegalArgumentException("height" + "must be greater than 0");
        }

        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        if (height < 1) {
            throw new IllegalArgumentException("height" + "must be greater than 0");
        }

        this.height = height;
    }

    public final NamingMode getSubtitleNamingMode() {
        return subtitleNamingMode;
    }

    public final void setSubtitleNamingMode(NamingMode subtitleNamingMode) {
        if (subtitleNamingMode == null) {
            this.subtitleNamingMode = NamingMode.NONE;
        }
        else {
            this.subtitleNamingMode = subtitleNamingMode;
        }
    }

    public final void addData(DataDefinition dataDefinition) {
        data.add(dataDefinition);
    }

    public final Iterable<DataDefinition> getData() {
        return java.util.Collections.unmodifiableList(data);
    }

    @Override
    public final String toString() {
        if ("".equals(shortName)) {
            if ("".equals(title)) {
                return "<untitled chart>";
            }
            else {
                return title;
            }
        }
        else {
            return shortName;
        }
    }
}
