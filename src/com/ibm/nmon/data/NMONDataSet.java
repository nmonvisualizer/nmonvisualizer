package com.ibm.nmon.data;

import java.util.Map;

public final class NMONDataSet extends ProcessDataSet {
    private final Map<String, String> metadata = new java.util.TreeMap<String, String>();
    private final Map<String, String> systemInfo = new java.util.TreeMap<String, String>();

    public NMONDataSet(String sourceFile) {
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

    Map<String, String> getSystemInfo() {
        return java.util.Collections.unmodifiableMap(systemInfo);
    }

    public String getSystemInfo(String name) {
        return systemInfo.get(name);
    }

    public int getSystemInfoCount() {
        return systemInfo.size();
    }

    public Iterable<String> getSystemInfoNames() {
        return java.util.Collections.unmodifiableSet(systemInfo.keySet());
    }

    public void setSystemInfo(String name, String value) {
        if ((name != null) && !"".equals(name) && (value != null) && !"".equals(value)) {
            systemInfo.put(name, value);
        }
    }

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
