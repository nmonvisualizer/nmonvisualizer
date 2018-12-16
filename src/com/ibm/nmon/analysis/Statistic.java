package com.ibm.nmon.analysis;

import com.ibm.nmon.data.DataType;

/**
 * Statistics supported by {@link AnalysisRecord}. This enum provides a generic way to query
 * analysis data via the {@link #getValue(AnalysisRecord, DataType, String) getValue()} method.
 */
public enum Statistic {
    AVERAGE("Average") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getAverage(type, fieldName);
        }
    },
    WEIGHTED_AVERAGE("Weighted Average") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getWeightedAverage(type, fieldName);
        }
    },
    MINIMUM("Minimum") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getMinimum(type, fieldName);
        }
    },
    MAXIMUM("Maximum") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getMaximum(type, fieldName);
        }
    },
    GRANULARITY_MAXIMUM("Granularity Max") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getGranularityMaximum(type, fieldName);
        }

        public String getName(int granularity) {
            granularity /= 1000;
            int minutes = granularity / 60;
            int seconds = granularity % 60;

            String title = null;

            if (minutes == 0) {
                title = seconds + "s Peak";
            }
            else {
                title = minutes + ":" + (seconds < 10 ? "0" : "") + seconds + " Peak (mm:ss)";
            }

            return title;
        }
    },
    STD_DEV("Std Dev") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getStandardDeviation(type, fieldName);
        }
    },
    MEDIAN("Median") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getMedian(type, fieldName);
        }
    },
    PERCENTILE_95("95th Percentile") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.get95thPercentile(type, fieldName);
        }
    },
    PERCENTILE_99("99th Percentile") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.get99thPercentile(type, fieldName);
        }
    },
    SUM("Sum") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getSum(type, fieldName);
        }
    },
    COUNT("Count") {
        @Override
        public double getValue(AnalysisRecord record, DataType type, String fieldName) {
            return record.getCount(type, fieldName);
        }
    };

    private final String name;

    private Statistic(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    };

    public abstract double getValue(AnalysisRecord record, DataType type, String fieldName);

    /**
     * Gets the statistic name.
     * 
     * @param granularity only required for {@link #GRANULARITY_MAXIMUM}
     * 
     * @return the name
     */
    public String getName(int granularity) {
        return name;
    }
}
