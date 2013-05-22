package com.ibm.nmon.data;

/**
 * A DataType that defines a secondary identifier. The primary and secondary ids are combined so
 * that calls to <code>getId()</code> return a combination of both ids.
 */
public final class SubDataType extends DataType {
    private final String subId;
    private final String asString;

    public SubDataType(String id, String subId, String name, String... fields) {
        this(id, subId, name, true, fields);
    }

    public SubDataType(String id, String subId, String name, boolean displayPrimaryId, String... fields) {
        super(buildId(id, subId), name, fields);

        if ((subId == null) || subId.equals("")) {
            throw new IllegalArgumentException("subId" + " cannot be empty");
        }

        this.subId = subId;

        if (displayPrimaryId) {
            this.asString = buildId(id, subId);
        }
        else {
            this.asString = subId;
        }
    }

    public String getPrimaryId() {
        int idx = getId().indexOf(" (");

        return getId().substring(0, idx);
    }

    public String getSubId() {
        return subId;
    }

    public static String buildId(String id, String subId) {
        return id + " (" + subId + ')';
    }

    @Override
    public String toString() {
        return asString;
    }
}
