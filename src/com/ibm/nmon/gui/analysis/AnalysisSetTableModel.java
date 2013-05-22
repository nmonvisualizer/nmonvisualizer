package com.ibm.nmon.gui.analysis;

import java.util.List;

import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.table.ChoosableColumnTableModel;

import com.ibm.nmon.analysis.AnalysisSetListener;
import com.ibm.nmon.analysis.AnalysisSet;

import com.ibm.nmon.data.DataSetListener;
import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;


/**
 * Base table model that maps a row number to a measurement in an AnalysisSet. Updates to the model
 * are handled as an AnalysisSetListener.
 */
public abstract class AnalysisSetTableModel extends ChoosableColumnTableModel implements DataSetListener,
        AnalysisSetListener {
    protected final NMONVisualizerGui gui;

    protected final AnalysisSet analysisSet;

    protected final List<String> keys = new java.util.LinkedList<String>();

    protected AnalysisSetTableModel(NMONVisualizerGui gui, AnalysisSet analysisSet) {
        super();

        this.gui = gui;
        this.analysisSet = analysisSet;

        gui.addDataSetListener(this);
        analysisSet.addListener(this);
    }

    public String getKey(int index) {
        return keys.get(index);
    }

    @Override
    public int getRowCount() {
        return keys.size();
    }

    @Override
    public final void analysisAdded(DataType type) {
        int startIdx = keys.size();

        for (String field : type.getFields()) {
            keys.add(type.getKey(field));
        }

        fireTableRowsInserted(startIdx, keys.size() - 1);
    }

    @Override
    public final void analysisAdded(DataType type, String field) {
        int startIdx = keys.size();

        keys.add(type.getKey(field));

        fireTableRowsInserted(startIdx, startIdx);
    }

    @Override
    public final void analysisRemoved(DataType type) {
        for (String field : type.getFields()) {
            analysisRemoved(type, field);
        }
    }

    @Override
    public final void analysisRemoved(DataType type, String field) {
        keys.remove(type.getKey(field));
        // may be inefficient since updates are called on each row
        fireTableDataChanged();
    }

    @Override
    public final void analysisCleared() {
        keys.clear();
        fireTableDataChanged();
    }

    @Override
    public void dataAdded(DataSet data) {
        fireTableDataChanged();
    }

    @Override
    public void dataRemoved(DataSet data) {
        fireTableDataChanged();
    }

    @Override
    public void dataChanged(DataSet data) {
        fireTableDataChanged();
    }

    @Override
    public void dataCleared() {
        keys.clear();
        fireTableDataChanged();
    }
}
