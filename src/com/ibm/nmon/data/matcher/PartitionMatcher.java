package com.ibm.nmon.data.matcher;

import java.util.List;

import org.slf4j.Logger;

import com.ibm.nmon.data.DataType;
import com.ibm.nmon.util.DataHelper;

/**
 * Matches <code>DISK</code> {@link DataType DataTypes} where the field defines a disk partition.
 * 
 * @see DataHelper#isNotPartition(String)
 */
public final class PartitionMatcher implements FieldMatcher {
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(PartitionMatcher.class);
    public static final PartitionMatcher INSTANCE = new PartitionMatcher();

    private PartitionMatcher() {}

    @Override
    public List<String> getMatchingFields(DataType type) {
        List<String> fields = new java.util.ArrayList<String>(type.getFieldCount() / 2);

        if (!type.getId().startsWith("DISK")) {
            logger.warn("specified partitions for a non-disk data type {}, ignoring", type);
        }
        else {
            for (String field : type.getFields()) {
                if (!DataHelper.isNotPartition(field) && !"Total".equals(field)) {
                    fields.add(field);
                }
            }
        }

        return fields;
    }

    @Override
    public String toString() {
        return "$PARTITIONS";
    }
}
