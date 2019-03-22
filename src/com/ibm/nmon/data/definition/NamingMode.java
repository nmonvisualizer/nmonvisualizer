package com.ibm.nmon.data.definition;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.interval.Interval;

import java.util.regex.Pattern;

/**
 * Defines the various ways output data can be named. This enumeration is used in conjunction with the
 * <code>rename</code> methods of {@link DataDefinition} so that labels (e.g. chart items) can be uniquely named based
 * on the matched data.
 */
public enum NamingMode {
    HOST() {
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameHost(data);
        }
    },
    HOST_TYPE() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameHost(data) + SEPARATOR + definition.renameType(type);
        }
    },
    HOST_FIELD() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameHost(data) + SEPARATOR + definition.renameField(field);
        }
    },
    HOST_STAT() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameHost(data) + SEPARATOR + definition.getStatistic().getName(granularity);
        }
    },
    TYPE() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameType(type);
        }
    },
    TYPE_FIELD() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameType(type) + SEPARATOR + definition.renameField(field);
        }
    },
    TYPE_STAT() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameType(type) + SEPARATOR + definition.getStatistic().getName(granularity);
        }
    },
    FIELD() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameField(field);
        }
    },
    FIELD_STAT() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.renameField(field) + SEPARATOR + definition.getStatistic().getName(granularity);
        }
    },
    STAT() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return definition.getStatistic().getName(granularity);
        }
    },
    DATE() {
        // regexes for variable replacement in date format
        // note these _must_ be enclosed by '
        private final Pattern HOST = Pattern.compile("HOST");
        private final Pattern TYPE = Pattern.compile("TYPE");
        private final Pattern FIELD = Pattern.compile("FIELD");
        private final Pattern STAT = Pattern.compile("STAT");

        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            String formatted = "";

            if (Interval.DEFAULT.equals(interval)) {
                formatted = definition.getDateFormat().format(data.getStartTime());
            }
            else {
                formatted = definition.getDateFormat().format(interval.getStart());
            }

            if (data != null) {
                formatted = HOST.matcher(formatted).replaceAll(definition.renameHost(data));
            }
            if (type != null) {
                formatted = TYPE.matcher(formatted).replaceAll(definition.renameType(type));
            }
            if (field != null) {
                formatted = FIELD.matcher(formatted).replaceAll(definition.renameField(field));
            }

            formatted = STAT.matcher(formatted).replaceAll(definition.getStatistic().getName(granularity));
            
            return formatted;
        }
    },
    NONE() {
        @Override
        public String getName(DataDefinition definition, DataSet data, DataType type, String field, Interval interval,
                int granularity) {
            return "";
        }
    };

    public abstract String getName(DataDefinition definition, DataSet data, DataType type, String field,
            Interval interval, int granularity);

    private static final String SEPARATOR = "-";
}
