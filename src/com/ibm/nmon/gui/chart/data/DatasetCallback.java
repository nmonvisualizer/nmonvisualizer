package com.ibm.nmon.gui.chart.data;

interface DatasetCallback {
    int getDataCount();

    int getItemCount(int dataIdx);

    double getValue(int dataIdx, int itemIdx);
}
