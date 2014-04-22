package com.ibm.nmon.gui.util;

import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataSetListener;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

/**
 * <p>
 * Combo box model for displaying a list of host names. When initialized, it will contain the
 * hostnames of all parsed <code>DataSets</code>s. As a {@link DataSetListener}, the model will
 * maintain the correct list as data is modified.
 * </p>
 * 
 * <p>
 * Note that this does not preclude using this model in an editable list. This model
 * <em>does not</em> create new data sets when hostnames are added nor does it attempt to rename the
 * host in an existing dataset when it is changed.
 * </p>
 */
public final class HostnameComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String>,
        DataSetListener {
    private static final long serialVersionUID = 6370252527474683605L;

    private final List<String> hosts = new java.util.LinkedList<String>();

    private String selected = null;

    public HostnameComboBoxModel(NMONVisualizerGui gui) {
        for (DataSet data : gui.getDataSets()) {
            addHostname(data.getHostname());
        }

        if (gui.getDataSetCount() == 1) {
            setSelectedItem(hosts.get(0));
        }

        gui.addDataSetListener(this);
    }

    @Override
    public String getElementAt(int index) {
        return hosts.get(index);
    }

    @Override
    public int getSize() {
        return hosts.size();
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        String item = (String) anItem;
        item = item.trim();

        // allow selected to be empty, but do not set that as a valid hostname
        selected = item;

        if (!"".equals(item) && !hosts.contains(selected)) {
            addHostname(selected);
        }
    }

    @Override
    public void dataAdded(DataSet data) {
        String hostname = data.getHostname();

        addHostname(hostname);
    }

    private void addHostname(String hostname) {
        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).equals(hostname)) {
                return;
            }

            if (hosts.get(i).compareTo(hostname) > 0) {
                hosts.add(i, hostname);

                fireIntervalAdded(this, i, i);

                return;
            }
        }

        // have not added yet, just add at the end
        hosts.add(hostname);
        fireIntervalAdded(this, 0, 0);
    }

    @Override
    public void dataRemoved(DataSet data) {
        String hostname = data.getHostname();

        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).equals(hostname)) {
                hosts.remove(i);

                fireIntervalRemoved(this, i, i);

                return;
            }
        }
    }

    @Override
    public void dataChanged(DataSet data) {}

    @Override
    public void dataCleared() {
        hosts.clear();

        fireContentsChanged(this, 1, Integer.MAX_VALUE);
    }
}
