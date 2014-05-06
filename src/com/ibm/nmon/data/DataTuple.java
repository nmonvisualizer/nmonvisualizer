package com.ibm.nmon.data;

/**
 * Simple holder for a DataSet, DataType and field combination.
 */
public final class DataTuple {
    private final DataSet dataSet;
    private final DataType type;
    private final String field;

    public DataTuple(DataSet dataSet, DataType type, String field) {
        if (dataSet == null) {
            throw new IllegalArgumentException("data set" + " cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("data type" + " cannot be null");
        }

        // allow field to be null to imply all fields

        this.dataSet = dataSet;
        this.type = type;
        this.field = field;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public DataType getDataType() {
        return type;
    }

    public String getField() {
        return field;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        else if (obj.getClass() == this.getClass()) {
            DataTuple t = (DataTuple) obj;

            if (dataSet.equals(t.getDataSet())) {
                if (getDataType().equals(t.getDataType())) {
                    if (getField() == null) {
                        return null == t.getField();
                    }
                    else {
                        return getField().equals(t.getField());
                    }
                }
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return dataSet + ": " + type + (field == null ? "-" + field : "");
    }

    @Override
    public int hashCode() {
        int code = getDataSet().hashCode();
        code = code * 59 + getDataType().hashCode();

        if (getField() != null) {
            code = code * 73 + getField().hashCode();
        }

        return code;
    }
}
