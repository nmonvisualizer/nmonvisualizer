package com.ibm.nmon.analysis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.nmon.data.DataType;

/**
 * <p>
 * Track a set of data to analyze. Fields in the set are identified by key, as in
 * {@link com.ibm.nmon.data.DataType#getKey(String) DataType.getKey()}. Data added to or removed
 * from the set will fire {@link AnalysisSetListener} events.
 * </p>
 * 
 * <p>
 * This class does not actually analyze or store any data. It is meant to be a holder for a set of
 * data that is being analyzed. To retrieve statistics, this class should be used in conjunction
 * with an {@link AnalysisRecord}, which actually contains the analyzed data.
 * </p>
 */
public final class AnalysisSet {
    // map DataType.getKey() values to the DataType and the field; these will be used in tandem
    private final Map<String, DataType> types = new java.util.HashMap<String, DataType>();
    private final Map<String, String> fields = new java.util.HashMap<String, String>();

    private final List<AnalysisSetListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<AnalysisSetListener>();

    public void addData(DataType type) {
        Set<String> added = new java.util.HashSet<String>(type.getFieldCount());

        for (String field : type.getFields()) {
            String key = type.getKey(field);

            if (types.put(key, type) == null) {
                fields.put(key, field);
                added.add(key);
            }
        }

        if (added.size() == type.getFieldCount()) {
            for (AnalysisSetListener listener : listeners) {
                listener.analysisAdded(type);
            }
        }
        else {
            for (String key : added) {
                for (AnalysisSetListener listener : listeners) {
                    listener.analysisAdded(types.get(key), fields.get(key));
                }
            }
        }
    }

    public void addData(DataType type, String field) {
        String key = type.getKey(field);

        if (types.put(key, type) == null) {
            fields.put(key, field);

            for (AnalysisSetListener listener : listeners) {
                listener.analysisAdded(type, field);
            }
        }
    }

    public void removeData(String key) {
        DataType type = types.remove(key);
        String field = fields.remove(key);

        if ((type != null) && (field != null)) {
            for (AnalysisSetListener listener : listeners) {
                listener.analysisRemoved(type, field);
            }
        }
    }

    public void clearData() {
        types.clear();
        fields.clear();

        for (AnalysisSetListener listener : listeners) {
            listener.analysisCleared();
        }
    }

    public DataType getType(String key) {
        return types.get(key);
    }

    public String getField(String key) {
        return fields.get(key);
    }

    public Iterable<String> getKeys() {
        return java.util.Collections.unmodifiableSet(types.keySet());
    }

    public int size() {
        return types.size();
    }

    public void addListener(AnalysisSetListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AnalysisSetListener listener) {
        listeners.remove(listener);
    }
}
