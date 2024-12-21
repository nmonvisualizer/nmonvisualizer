package com.ibm.nmon.data.transform;

import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;

/**
 * <p>
 * Changes Windows Perfmon data from Bytes to KB for system, network and disk data and MB for
 * memory.
 * </p>
 * <p>
 * This class maintains state internally, so {@link #reset()} should be called after parsing a
 * particular file.
 * <p>
 */
public class WindowsBytesTransform implements DataTransform {
    private static final Matcher VALID_TYPES = Pattern.compile(
            "LogicalDisk.*|PhysicalDisk.*|Network Interface.*|Memory|System").matcher("");
    // match all Bytes, but not KBytes, MBytes, etc
    private static final Matcher VALID_FIELDS = Pattern.compile("([^%].*?[^KMGTEP])?Bytes(.*)").matcher("");

    private Map<String, Set<Integer>> changedFields = new java.util.HashMap<String, Set<Integer>>();

    @Override
    public DataType buildDataType(String id, String subId, String name, String... fields) {
        Set<Integer> changes = new java.util.HashSet<Integer>(fields.length);

        for (int i = 0; i < fields.length; i++) {
            VALID_FIELDS.reset(fields[i]);

            if (VALID_FIELDS.matches()) {
                if ("Memory".equals(id)) {
                    fields[i] = VALID_FIELDS.replaceAll("$1MB$2");
                }
                else {
                    fields[i] = VALID_FIELDS.replaceAll("$1KB$2");
                }

                changes.add(i);
            }
        }

        changedFields.put(name, changes);

        if (subId != null) {
            return new SubDataType(id, subId, name, fields);
        }
        else {
            return new DataType(id, name, fields);
        }
    }

    @Override
    public double[] transform(DataType type, double[] data) {
        Set<Integer> changes = changedFields.get(type.getName());

        if (changes != null) {
            for (int i = 0; i < data.length; i++) {
                if (changes.contains(i)) {
                    if ("Memory".equals(type.getId())) {
                        data[i] /= 1024 * 1024;
                    }
                    else {
                        data[i] /= 1024;
                    }
                }
            }
        }

        return data;
    }

    @Override
    public boolean isValidFor(String typeId, String subId) {
        return VALID_TYPES.reset(typeId).matches();
    }

    public void reset() {
        changedFields.clear();
    }
}
