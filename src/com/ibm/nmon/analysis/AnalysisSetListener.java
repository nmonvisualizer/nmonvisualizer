package com.ibm.nmon.analysis;

import com.ibm.nmon.data.DataType;

public interface AnalysisSetListener {
    public void analysisAdded(DataType type);

    public void analysisAdded(DataType type, String field);

    public void analysisRemoved(DataType type);

    public void analysisRemoved(DataType type, String field);

    public void analysisCleared();
}
