package com.ibm.nmon.data;

import java.util.Map;

/**
 * Holder for data from a specific time, identified by either a timestamp (TXXXX) or the
 * corresponding time in milliseconds. DataRecords can hold any amount of data as long as that data
 * is all from the same time.
 */
public final class DataRecord implements Comparable<DataRecord> {
    private long time;
    private final String timestamp;

    // associate the DataType with the set of values for this record's timestamp
    private final Map<DataType, double[]> values = new java.util.HashMap<DataType, double[]>();

    public DataRecord(long time, String timestamp) {
        this.time = time;

        if ((timestamp == null) || timestamp.equals("")) {
            throw new IllegalArgumentException("timstamp cannot be empty");
        }

        this.timestamp = timestamp;
    }

    public long getTime() {
        return time;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // only called by DataSet.adjustTimes

    final void adjustTime(long adjustmentMillis) {
        if (adjustmentMillis == 0) {
            return;
        }
        else {
            time += adjustmentMillis;
        }
    }

    /*
     * Potential future bug here since setValue creates an array full of NaNs but addData uses
     * ArrayPool (all zeros). The existing parsers use one or the other, and do not mix calls in the
     * same parse.
     * 
     * More importantly, these functions DO NOT make any provisions for updates; DataRecords are
     * assumed read only after parsing. If that is not the case, code needs to be added to create a
     * new array so that _all_ the references to ArrayPool arrays are not changed.
     */
    public void setValue(DataType type, String field, double value) {
        // check if type has field first
        int idx = type.getFieldIndex(field);

        double[] data = values.get(type);

        if (data == null) {
            data = new double[type.getFieldCount()];

            java.util.Arrays.fill(data, Double.NaN);

            values.put(type, data);
        }

        data[idx] = value;
    }

    public void addData(DataType type, double[] data) {
        if (values.containsKey(type.getId())) {
            throw new IllegalArgumentException("DataType " + type.getId() + " already defined for timestamp "
                    + timestamp);
        }
        else {
            if (data.length < type.getFieldCount()) {
                throw new IllegalArgumentException("DataType " + type.getId() + " defines " + type.getFieldCount()
                        + " fields but there are only " + data.length + " values recorded for timestamp " + timestamp);
            }

            values.put(type, ArrayPool.getArray(data));
        }
    }

    public boolean removeData(DataType type) {
        return values.remove(type) != null;
    }

    public double getData(DataType type, String fieldName) {
        double[] data = values.get(type);

        if (data == null) {
            throw new IllegalArgumentException("record does not contain any data for DataType " + type.getId());
        }

        int fieldIndex = type.getFieldIndex(fieldName);

        // allow ArrayIndexOutOfBoundsException here because that implies the DataType has
        // changed, which should not happen
        return data[fieldIndex];
    }

    /**
     * Return the raw data for the given DataType. This array <em>is not</em> copied, so care must
     * be taken to not update or otherwise invalidate the data.
     */
    public double[] getData(DataType type) {
        double[] data = values.get(type);

        if (data == null) {
            throw new IllegalArgumentException("record does not contain any data for DataType " + type.getId());
        }

        return data;
    }

    public boolean hasData(DataType type) {
        return values.containsKey(type);
    }

    @Override
    public String toString() {
        return "{timestamp=" + getTimestamp() + ", dataTypes=" + values.keySet() + '}';
    }

    @Override
    public final int compareTo(DataRecord r) {
        return (int) (time - r.time);
    }
}
