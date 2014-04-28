package com.ibm.nmon.analysis;

import org.slf4j.Logger;

import java.lang.ref.SoftReference;

import java.util.Map;
import java.util.List;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;

import com.ibm.nmon.interval.Interval;
import com.ibm.nmon.util.TimeFormatCache;

/**
 * <p>
 * Holder class for analyzing data from a specific DataSet. If the DataSet does not have data for
 * the given DataType / field combination all methods in this class will return <code>NaN</code>
 * except for {@link #getCount(DataType, String) getCount()}, which returns zero.
 * </p>
 * 
 * <p>
 * This class caches statistics for measurements during a given Interval rather than recalculating
 * from the raw data each time. Calculations are done lazily, when a statistic is requested, not
 * when a measurement is added to the record. Data is cached as SoftReference objects, so while this
 * class could potentially use a large amount of memory, it should not cause OutOfMemoryExceptions.
 * </p
 */
public final class AnalysisRecord {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AnalysisRecord.class);

    // struct for holding analyzed data
    // data is analyzed lazily, but everything is calculated on the first call, not for each get
    // method
    private static final class AnalysisHolder {
        int count = 0;
        double sum = 0;

        double average = Double.NaN;
        double granularityMaximum = Double.MIN_VALUE;

        double median = Double.NaN;
        double percentile95 = Double.NaN;
        double percentile99 = Double.NaN;

        double minimum = Double.MAX_VALUE;
        double maximum = Double.MIN_VALUE;

        double standardDeviation = Double.NaN;
    }

    private final DataSet data;

    // associate DataType keys with the set of values for this record
    private final Map<String, SoftReference<AnalysisHolder>> values = new java.util.HashMap<String, SoftReference<AnalysisHolder>>();

    private Interval interval;

    private int granularity = 60000;

    public AnalysisRecord(DataSet data) {
        this.data = data;
        this.interval = Interval.DEFAULT;
    }

    public DataSet getDataSet() {
        return data;
    }

    public Interval getInterval() {
        return interval;
    }

    public void setInterval(Interval interval) {
        if (!this.interval.equals(interval)) {
            this.interval = interval;

            values.clear();
        }
    }

    public void setGranularity(int granularity) {
        if (granularity < 1) {
            throw new IllegalArgumentException("granularity must be greater than 0");
        }

        if (granularity != this.granularity) {
            this.granularity = granularity;

            values.clear();
        }
    }

    public double getAverage(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).average;
    }

    public double getMinimum(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).minimum;
    }

    public double getMaximum(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).maximum;
    }

    public double getGranularityMaximum(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).granularityMaximum;
    }

    public double getMedian(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).median;
    }

    public double get95thPercentile(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).percentile95;
    }

    public double get99thPercentile(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).percentile99;
    }

    public double getStandardDeviation(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).standardDeviation;
    }

    public double getSum(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).sum;
    }

    public int getCount(DataType type, String fieldName) {
        return analyzeIfNecessary(type, fieldName).count;
    }

    private AnalysisHolder analyzeIfNecessary(DataType type, String fieldName) {
        if (type == null) {
            throw new IllegalArgumentException("cannot analyze null " + " type");
        }

        if ((fieldName == null) || "".equals(fieldName)) {
            throw new IllegalArgumentException("cannot analyze null " + "field");
        }

        String key = type.getKey(fieldName);

        SoftReference<AnalysisHolder> holderRef = values.get(key);
        AnalysisHolder holder = null;

        if (holderRef != null) {
            holder = holderRef.get();

            if (holder != null) {
                return holder;
            }
            // else valid SoftReference but the actual holder has been GC'ed so recreate it
        }

        holder = new AnalysisHolder();
        values.put(key, new SoftReference<AnalysisHolder>(holder));

        long startT = System.nanoTime();

        DataType typeToAnalyze = data.getType(type.getId());

        if ((typeToAnalyze != null) && typeToAnalyze.hasField(fieldName)) {
            // depending on the Interval, all DataRecords may not be processed, but assume
            // over-allocating here is faster than forcing some number of array resizes
            List<Double> allValues = new java.util.ArrayList<Double>(data.getRecordCount());

            long lastGranularityTime = Math.max(interval.getStart(), data.getStartTime());
            int countSinceLastGranularity = 0;
            double granularityTotal = 0;

            for (DataRecord dataRecord : data.getRecords(interval)) {
                if (dataRecord.hasData(typeToAnalyze)) {
                    double value = dataRecord.getData(typeToAnalyze, fieldName);

                    if (Double.isNaN(value)) {
                        continue;
                    }

                    holder.sum += value;

                    if (value > holder.maximum) {
                        holder.maximum = value;
                    }

                    if (value < holder.minimum) {
                        holder.minimum = value;
                    }

                    allValues.add(value);

                    ++countSinceLastGranularity;
                    granularityTotal += value;

                    if ((dataRecord.getTime() - lastGranularityTime) >= granularity) {
                        double peakAverage = granularityTotal / countSinceLastGranularity;

                        if (peakAverage > holder.granularityMaximum) {
                            holder.granularityMaximum = peakAverage;
                        }

                        countSinceLastGranularity = 0;
                        granularityTotal = 0;

                        lastGranularityTime = dataRecord.getTime();
                    }
                }
            }

            if (allValues.size() > 0) {
                holder.count = allValues.size();
                holder.average = holder.sum / holder.count;

                java.util.Collections.sort(allValues);

                holder.median = calculatePercentile(.5, allValues);
                holder.percentile95 = calculatePercentile(.95, allValues);
                holder.percentile99 = calculatePercentile(.99, allValues);

                double sumSqDiffs = 0;

                for (double value : allValues) {
                    sumSqDiffs += Math.pow(value - holder.average, 2);
                }

                holder.standardDeviation = Math.sqrt(sumSqDiffs / holder.count);
            }
            else {
                // file has data, but not for the given interval
                // set all values to NaN
                holder.maximum = Double.NaN;
                holder.minimum = Double.NaN;

                holder.granularityMaximum = Double.NaN;
            }
        }
        else {
            // typeToAnalyze is null or type does not have the field
            // just return a holder full of NaNs
            holder.maximum = Double.NaN;
            holder.minimum = Double.NaN;

            holder.granularityMaximum = Double.NaN;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}: {}-{} analyzed for {} in {}ms ",
                    new Object[] { data, type, fieldName, TimeFormatCache.formatInterval(interval),
                            (System.nanoTime() - startT) / 1000000.0d });
        }

        return holder;
    }

    public static double calculatePercentile(double percentile, List<Double> allValues) {
        double n = allValues.size() * percentile;
        int idx = (int) n;

        if ((n - idx) == 0) {
            return (allValues.get(idx) + allValues.get(idx - 1)) / 2;
        }
        else {
            return allValues.get(idx);
        }
    }
}
