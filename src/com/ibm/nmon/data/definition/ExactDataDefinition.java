package com.ibm.nmon.data.definition;

import java.util.Collection;
import java.util.List;

import com.ibm.nmon.analysis.Statistic;
import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

/**
 * A {@link DataDefinition} that matches a pre-defined set of data. This class supports matches on a
 * single {@link DataSet}, a single {@link DataType} and a set of fields.
 */
public final class ExactDataDefinition extends DataDefinition {
    private final List<DataSet> dataSets;
    private final List<DataType> types;
    private final List<String> fields;

    public ExactDataDefinition(DataSet data, DataType type, List<String> fields, Statistic stat,
            boolean useSecondaryYAxis) {
        super(stat, useSecondaryYAxis);

        this.dataSets = java.util.Collections.singletonList(data);
        this.types = java.util.Collections.singletonList(type);
        this.fields = fields;
    }

    public ExactDataDefinition(DataSet data, DataType type, List<String> fields) {
        super();

        this.dataSets = java.util.Collections.singletonList(data);
        this.types = java.util.Collections.singletonList(type);
        this.fields = fields;
    }

    public DataSet getDataSet() {
        return dataSets.get(0);
    }

    public DataType getDataType() {
        return types.get(0);
    }

    @Override
    public boolean matchesHost(DataSet data) {
        return data.equals(this.dataSets.get(0));
    }

    @Override
    public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch) {
        return dataSets;
    }

    @Override
    public List<DataType> getMatchingTypes(DataSet data) {
        return types;
    }

    @Override
    public List<String> getMatchingFields(DataType type) {
        return fields;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(128);

        builder.append('{');

        builder.append("data: ");
        builder.append(dataSets);

        builder.append("; ");

        builder.append("types: ");
        builder.append(types);

        builder.append("; ");

        builder.append("fields: ");
        builder.append(fields);

        builder.append('}');

        return builder.toString();
    }
}
