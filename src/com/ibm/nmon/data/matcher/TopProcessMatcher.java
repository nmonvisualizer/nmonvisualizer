package com.ibm.nmon.data.matcher;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import java.util.Comparator;

import com.ibm.nmon.NMONVisualizerApp;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.ProcessDataSet;
import com.ibm.nmon.data.ProcessDataType;

import com.ibm.nmon.analysis.AnalysisRecord;

/**
 * Matches processes via {@link ProcessDataType} CPU utilization. The {@link AnalysisRecord#getWeightedAverage weighted
 * average} is calculated and the 10 processes with the highest utilization are matched.
 */
public final class TopProcessMatcher implements TypeMatcher {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TopProcessMatcher.class);

    public static final TopProcessMatcher INSTANCE = new TopProcessMatcher();
    private static final int TOP_N = 10;

    private TopProcessMatcher() {}

    private NMONVisualizerApp app;

    public void setApp(NMONVisualizerApp app) {
        this.app = app;
    }

    @Override
    public List<DataType> getMatchingTypes(DataSet data) {
        if (!(data instanceof ProcessDataSet)) {
            return java.util.Collections.emptyList();
        }

        long startT = System.nanoTime();

        // sort all processes by CPU utilization
        // this can be slow if there is a lot of data and / or processes since AnalysisRecords for all process types
        // will need to be calculated
        Map<Double, DataType> sortedByCPU = new java.util.TreeMap<Double, DataType>(COMPARATOR);

        AnalysisRecord analysis = app.getAnalysis(data);

        for (DataType type : data.getTypes()) {
            if (type instanceof ProcessDataType) {

                if (type.hasField("%CPU")) {
                    sortedByCPU.put(analysis.getWeightedAverage(type, "%CPU"), type);
                }
                else if (type.hasField("% Processor Time")) {
                    sortedByCPU.put(analysis.getWeightedAverage(type, "% Processor Time"), type);
                }
                // else ignore
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}: sorted {} processes for {} in {} ms", data, ((ProcessDataSet) data).getProcessCount(),
                    (System.nanoTime() - startT) / 1000000.0d);
        }

        // only return the TOP_N processes, if there are that many
        List<DataType> types = new java.util.ArrayList<DataType>();
        int n = 1;

        for (DataType type : sortedByCPU.values()) {
            if (n > TOP_N) {
                break;
            }

            types.add(type);

            n++;
        }

        return types;
    }

    @Override
    public String toString() {
        return "$TOP_PROCESSES";
    }

    private static final Comparator<Double> COMPARATOR = new Comparator<Double>() {
        @Override
        public int compare(Double d1, Double d2) {
            // negate to sort descending; assume -0 == 0
            return -Double.compare(d1, d2);
        }
    };
}
