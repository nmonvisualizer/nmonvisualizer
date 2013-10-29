package com.ibm.nmon.data;

import java.util.TreeMap;
import java.util.Set;

import com.ibm.nmon.interval.Interval;

/**
 * <p>
 * A container for all the data in a single parsed file.
 * </p>
 * 
 * <p>
 * A data set will define a set of DataTypes and span some number of time intervals. For each
 * interval, there will a single DataRecord. Each record will contain at least one DataType with an
 * associated array of values.
 * </p>
 * 
 * Conceptually, the structure is as follows:
 * 
 * <pre>
 * DataSet
 *   time 1
 *     DataRecord
 *       DataType a
 *         value 1
 *         value 2
 *         ...
 *         value n
 *       DataType b
 *         value 1
 *         value 2
 *         ...
 *         value n
 *       ...
 *       DataType x
 *   time 2
 *     DataRecord
 *       DataType a
 *         value 1
 *         value 2
 *         ...
 *         value n
 *       DataType b
 *         value 1
 *         value 2
 *         ...
 *         value n
 *       ...
 *       DataType x
 *   ...
 *   time n
 * </pre>
 */
public abstract class DataSet implements Comparable<DataSet> {
    private final TreeMap<String, DataType> dataTypes = new TreeMap<String, DataType>();

    // associate data with each timestamp
    private final TreeMap<Long, DataRecord> data = new TreeMap<Long, DataRecord>();

    public abstract String getHostname();

    public abstract void setHostname(String hostname);

    public abstract String getSourceFile();

    public final void addType(DataType type) {
        if (type != null) {
            if (dataTypes.containsKey(type.getId())) {
                throw new IllegalArgumentException("cannot redefine DataType " + type.getId()
                        + " within the same data set");
            }
            else {
                dataTypes.put(type.getId(), type);
            }
        }
    }

    // callers are responsible fore removing the type with any associated DataRecords
    final void removeType(DataType type) {
        if (type != null) {
            dataTypes.remove(type.getId());
        }
    }

    public final boolean containsType(String typeId) {
        return dataTypes.containsKey(typeId);
    }

    public final DataType getType(String typeId) {
        return dataTypes.get(typeId);
    }

    /**
     * @return all the DataTypes defined in this data set
     */
    public final Iterable<DataType> getTypes() {
        return java.util.Collections.unmodifiableCollection(dataTypes.values());
    }

    public final int getTypeCount() {
        return dataTypes.size();
    }

    public final void addRecord(DataRecord record) {
        if (record != null) {
            data.put(record.getTime(), record);
        }
    }

    /**
     * @return the number of DataRecords in this data set.
     */
    public final int getRecordCount() {
        return data.size();
    }

    public final int getRecordCount(Interval interval) {
        return data.subMap(interval.getStart(), true, interval.getEnd(), true).size();
    }

    public final DataRecord getRecord(long time) {
        return data.get(time);
    }

    /**
     * @return all the DataRecords in this data set, sorted by time, earliest first.
     */
    public final Iterable<DataRecord> getRecords() {
        return java.util.Collections.unmodifiableCollection(data.values());
    }

    public final Iterable<DataRecord> getRecords(Interval interval) {
        return java.util.Collections.unmodifiableCollection(data.subMap(interval.getStart(), true, interval.getEnd(),
                true).values());
    }

    /**
     * @return all the timestamps recorded by this data set.
     */
    public final Set<Long> getTimes() {
        return java.util.Collections.unmodifiableSet(data.keySet());
    }

    public final long getStartTime() {
        return data.firstKey();
    }

    public final long getEndTime() {
        return data.size() == 0 ? Long.MIN_VALUE : data.lastKey();
    }

    public final void adjustTimes(long adjustmentMillis) {
        if (adjustmentMillis == 0) {
            return;
        }

        // recreate the data using the new times
        TreeMap<Long, DataRecord> newData = new TreeMap<Long, DataRecord>();

        for (long time : data.keySet()) {
            DataRecord record = data.get(time);
            record.adjustTime(adjustmentMillis);

            newData.put(time + adjustmentMillis, record);
        }

        // data is final, so recopy it back
        data.clear();
        data.putAll(newData);
    }

    @Override
    public final String toString() {
        return getHostname();
    }

    @Override
    public final int hashCode() {
        return (getHostname() + '|' + getStartTime() + '|' + getEndTime()).hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        else if (obj instanceof DataSet) {
            DataSet data = (DataSet) obj;

            return this.getHostname().equals(data.getHostname()) && (this.getStartTime() == data.getStartTime())
                    && (this.getEndTime() == data.getEndTime());
        }
        else {
            return false;
        }
    }

    @Override
    public final int compareTo(DataSet f) {
        int compare = this.getHostname().compareTo(f.getHostname());

        if (compare == 0) {
            if (this.data.isEmpty() && this.data.isEmpty()) {
                return 0;
            }
            else if (this.getStartTime() == f.getStartTime()) {
                if (this.getEndTime() == f.getEndTime()) {
                    return 0;
                }
                else {
                    return this.getEndTime() > f.getEndTime() ? 1 : -1;
                }
            }
            else {
                return this.getStartTime() > f.getStartTime() ? 1 : -1;
            }
        }
        else {
            return compare;
        }
    }
}
