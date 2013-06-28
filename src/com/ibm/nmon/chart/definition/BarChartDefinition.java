package com.ibm.nmon.chart.definition;

import java.util.List;

import com.ibm.nmon.data.definition.DataDefinition;
import com.ibm.nmon.data.definition.NamingMode;

/**
 * Defines a bar chart which has a labeled category axis. Bars are defined as a collection of
 * {@link DataDefinition DataDefinitions}. Bars are named by two {@link NamingMode NamingModes}; one
 * defines the category name, the other the bar name.
 */
public final class BarChartDefinition extends YAxisChartDefinition {
    private final boolean subtractionNeeded;

    private String categoryAxisLabel = "";

    private NamingMode categoryNamingMode;
    private NamingMode barNamingMode;

    private final List<DataDefinition> categories;

    public BarChartDefinition(String shortName, String title) {
        this(shortName, title, true, false);
    }

    public BarChartDefinition(String shortName, String title, boolean subtractionNeeded) {
        this(shortName, title, true, subtractionNeeded);
    }

    public BarChartDefinition(String shortName, String title, boolean stacked, boolean subtractionNeeded) {
        super(shortName, title, stacked);

        this.subtractionNeeded = subtractionNeeded;

        categoryNamingMode = NamingMode.TYPE;
        barNamingMode = NamingMode.FIELD;

        categories = new java.util.ArrayList<DataDefinition>(3);
    }

    public boolean isSubtractionNeeded() {
        return subtractionNeeded;
    }

    public String getCategoryAxisLabel() {
        return categoryAxisLabel;
    }

    public void setCategoryAxisLabel(String categoryAxisLabel) {
        if (categoryAxisLabel == null) {
            this.categoryAxisLabel = "";
        }
        else {
            this.categoryAxisLabel = categoryAxisLabel;
        }
    }

    public NamingMode getBarNamingMode() {
        return barNamingMode;
    }

    public void setBarNamingMode(NamingMode mode) {
        if (mode == null) {
            barNamingMode = NamingMode.TYPE;
        }
        else {
            barNamingMode = mode;
        }
    }

    public NamingMode getCategoryNamingMode() {
        return categoryNamingMode;
    }

    public void setCategoryNamingMode(NamingMode mode) {
        if (mode == null) {
            categoryNamingMode = NamingMode.FIELD;
        }
        else {
            categoryNamingMode = mode;
        }
    }

    public void addCategory(DataDefinition category) {
        categories.add(category);
    }

    public Iterable<DataDefinition> getCategories() {
        return java.util.Collections.unmodifiableList(categories);
    }
}
