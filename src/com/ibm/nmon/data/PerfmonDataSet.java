package com.ibm.nmon.data;

import java.util.Map;

public final class PerfmonDataSet extends ProcessDataSet {
    private final Map<String, String> metadata = new java.util.TreeMap<String, String>();

    public PerfmonDataSet(String sourceFile) {
        if ((sourceFile == null) || "".equals(sourceFile)) {
            throw new IllegalArgumentException("sourceFile cannot be null");
        }

        metadata.put("source_file", sourceFile);
    }

    public String getHostname() {
        return metadata.get("host");
    }

    public void setHostname(String hostname) {
        if ((hostname == null) || "".equals(hostname)) {
            throw new IllegalArgumentException("hostname cannot be null");
        }

        metadata.put("host", hostname);
    }

    public String getSourceFile() {
        return metadata.get("source_file");
    }

    public String getTypeIdPrefix() {
        return "Process";
    };

    Map<String, String> getMetadata() {
        return java.util.Collections.unmodifiableMap(metadata);
    }

    public String getMetadata(String name) {
        return metadata.get(name);
    }

    public int getMetadataCount() {
        return metadata.size();
    }

    public Iterable<String> getMetadataNames() {
        return java.util.Collections.unmodifiableSet(metadata.keySet());
    }

    public void setMetadata(String name, String value) {
        if ((name != null) && !"".equals(name) && (value != null) && !"".equals(value)) {
            metadata.put(name, value);
        }
    }
}
