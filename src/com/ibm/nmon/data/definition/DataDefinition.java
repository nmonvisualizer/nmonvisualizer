package com.ibm.nmon.data.definition;

import java.util.Collection;
import java.util.List;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.matcher.HostMatcher;

import com.ibm.nmon.analysis.Statistic;

/**
 * Base class for programmatically defining a set of data. A definition can match any number of
 * hosts (via {@link DataSet DataSets}), {@link DataType DataTypes}, or fields. This class also
 * supports renaming these values as well. Finally, a {@link Statistic}, which defaults to
 * <code>AVERAGE</code>, can be specified for clients that need aggregated data.
 */
public abstract class DataDefinition {
    private final Statistic stat;

    private final boolean useSecondaryYAxis;

    public static DataDefinition ALL_DATA = new DefaultDataDefinition(null, null, null, null, false);

    protected DataDefinition() {
        this(null, false);
    }

    protected DataDefinition(Statistic stat, boolean useSecondaryYAxis) {
        if (stat == null) {
            stat = Statistic.AVERAGE;
        }

        this.stat = stat;
        this.useSecondaryYAxis = useSecondaryYAxis;
    }

    public Statistic getStatistic() {
        return stat;
    }

    public boolean usesSecondaryYAxis() {
        return useSecondaryYAxis;
    }

    /**
     * Does the definition match the given host?
     * 
     * @return <code>true</code>; by default matches all hosts
     */
    public boolean matchesHost(DataSet data) {
        return HostMatcher.ALL.matchesHost(data);
    }

    /**
     * Given a list of <code>DataSet</code>s, return a new list containing the ones that match this
     * definition.
     */
    public abstract List<DataSet> getMatchingHosts(Collection<DataSet> toMatch);

    /**
     * Given a list of <code>DataType</code>s, return a new list containing the ones that match this
     * definition.
     */
    public abstract List<DataType> getMatchingTypes(DataSet data);

    /**
     * Given a <code>DataType</code>, return a new list containing the fields that match this
     * definition.
     */
    public abstract List<String> getMatchingFields(DataType type);

    /**
     * Get a new hostname for the given <code>DataSet</code>.
     * 
     * @return {@link DataSet#getHostname()} by default
     */
    public String renameHost(DataSet data) {
        return data.getHostname();
    }

    /**
     * Get a new name for the given <code>DataType</code>.
     * 
     * @return {@link DataType#toString()} by default
     */
    public String renameType(DataType type) {
        return type.toString();
    }

    /**
     * Get a new name for the given field.
     * 
     * @return the same field name by default
     */
    public String renameField(String field) {
        return field;
    }
}
