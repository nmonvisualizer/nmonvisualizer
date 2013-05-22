package com.ibm.nmon.data;

public interface DataSetListener {
    public void dataAdded(DataSet data);

    public void dataRemoved(DataSet data);

    public void dataChanged(DataSet data);

    public void dataCleared();
}
