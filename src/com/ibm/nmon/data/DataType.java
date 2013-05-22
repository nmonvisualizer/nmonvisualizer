package com.ibm.nmon.data;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A DataType defines the field (column) names for a row of parsed data. Each DataType has a short
 * mnemonic id and a longer, descriptive name; both should be unique within a single parsed file. A
 * DataType also contains an ordered list of fields, which are also named. Fields are 0 indexed.
 * </p>
 * 
 * <p>
 * Rather than require both a DataType and a String to identify a field, this class also defines a
 * <code>key</code> for each field that can be used as a unique identifier in applications. It will
 * be up to applications to store the correct mapping between key and DataTypes and keys and fields,
 * but this class guarantees that keys will be unique as long as DataType ids are unique within an
 * parsed file.
 * </p>
 */
public class DataType {
    protected final String id;
    private final String name;

    // field names ordered as read from the parsed file
    private final List<String> orderedFields;
    // map field names to ordinal positions; 0 based
    private final Map<String, Integer> fields;

    // map field names to unique keys so type/field combos can be used efficiently in hashmaps
    private final Map<String, String> fieldKeys;

    public DataType(String id, String name, String... fields) {
        if ((id == null) || id.equals("")) {
            throw new IllegalArgumentException("id" + " cannot be empty");
        }

        if ((name == null) || name.equals("")) {
            throw new IllegalArgumentException("name" + " cannot be empty");
        }

        if (fields == null) {
            throw new IllegalArgumentException("fields" + " cannot be null");
        }

        if (fields.length == 0) {
            throw new IllegalArgumentException("fields" + " cannot be empty");
        }

        this.id = id;
        this.name = name;
        this.orderedFields = java.util.Collections.unmodifiableList(Arrays.asList(fields));

        this.fields = new java.util.HashMap<String, Integer>(fields.length);

        for (int i = 0; i < orderedFields.size(); i++) {
            this.fields.put(orderedFields.get(i), i);
        }

        this.fieldKeys = new java.util.HashMap<String, String>(fields.length);
    }

    public final String getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final int getFieldCount() {
        return orderedFields.size();
    }

    public final String getField(int index) {
        return orderedFields.get(index);
    }

    public final int getFieldIndex(String name) {
        Integer i = fields.get(name);

        if (i == null) {
            throw new IllegalArgumentException("DataType " + id + " does not have a field named " + name);
        }
        else {
            return i;
        }
    }

    public final boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public final List<String> getFields() {
        return orderedFields;
    }

    public final String getKey(String field) {
        String key = fieldKeys.get(field);

        if (key == null) {
            getFieldIndex(field); // throws error on invalid field
            key = getId() + ':' + field;
            fieldKeys.put(field, key);
        }

        return key;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        else if (o instanceof DataType) {
            DataType type = (DataType) o;

            return type.id.equals(this.id);
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
