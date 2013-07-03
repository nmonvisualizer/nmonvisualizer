package com.ibm.nmon.data.definition;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

/**
 * Defines the various ways output data can be named. This enumeration is used in conjunction with
 * the <code>rename</code> methods of {@link DataDefinition} so that labels (e.g. chart items) can
 * be uniquely named based on the matched data.
 * 
 */
public enum NamingMode {
    HOST() {
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameHost(data);
        }
    },
    HOST_TYPE() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameHost(data) + SEPARATOR + definition.renameType(type);
        }
    },
    HOST_FIELD() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameHost(data) + SEPARATOR + definition.renameField(field);
        }
    },
    HOST_STAT() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameHost(data) + SEPARATOR + definition.getStatistic().getName(granularity);
        }
    },
    TYPE() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameType(type);
        }
    },
    TYPE_FIELD() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameType(type) + SEPARATOR + definition.renameField(field);
        }
    },
    TYPE_STAT() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameType(type) + SEPARATOR + definition.getStatistic().getName(granularity);
        }
    },
    FIELD() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameField(field);
        }
    },
    FIELD_STAT() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.renameField(field) + SEPARATOR + definition.getStatistic().getName(granularity);
        }
    },
    STAT() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return definition.getStatistic().getName(granularity);
        }
    },
    NONE() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity) {
            return "";
        }
    };

    public abstract String getName(DataDefinition definition, DataSet data, DataType type, String field, int granularity);

    public static final String SEPARATOR = "-";
}
