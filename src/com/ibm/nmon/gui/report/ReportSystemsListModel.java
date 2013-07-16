package com.ibm.nmon.gui.report;

import java.util.List;

import javax.swing.AbstractListModel;

import com.ibm.nmon.data.DataSet;

final class ReportSystemsListModel extends AbstractListModel<DataSet> {
    private final List<DataSet> systems = new java.util.ArrayList<DataSet>();

    // see ReportFrame's ListCellRenderer which uses index 0 as 'All Systems'
    // support that here by reporting a larger size and modifying the index
    @Override
    public DataSet getElementAt(int index) {
        if (index == 0) {
            return null;
        }
        else {
            return systems.get(index - 1);
        }
    }

    @Override
    public int getSize() {
        return systems.size() + 1;
    }

    void addData(DataSet data) {
        systems.add(data);
        java.util.Collections.sort(systems);

        fireContentsChanged(this, getSize(), getSize());
    }

    void removeData(DataSet data) {
        systems.remove(data);

        fireContentsChanged(this, 1, getSize());
    }

    void clearData() {
        systems.clear();

        fireContentsChanged(this, 1, getSize());
    }

    void dataChanged() {
        fireContentsChanged(this, 1, getSize());
    }
}
