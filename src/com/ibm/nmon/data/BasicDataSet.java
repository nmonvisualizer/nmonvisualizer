package com.ibm.nmon.data;

import java.util.Map;

public class BasicDataSet extends DataSet {
    private final Map<String, String> metadata = new java.util.HashMap<String, String>();

    public BasicDataSet(String sourceFile) {
        if ((sourceFile == null) || "".equals(sourceFile)) {
            throw new IllegalArgumentException("sourceFile cannot be null");
        }

        metadata.put("source_file", sourceFile);
    }

    public final String getHostname() {
        return metadata.get("hostname");
    }

    @Override
    public void setHostname(String hostname) {
        if ((hostname == null) || "".equals(hostname)) {
            throw new IllegalArgumentException("hostname cannot be null");
        }

        metadata.put("hostname", hostname);
    }

    public String getSourceFile() {
        return metadata.get("source_file");
    }

    Map<String, String> getMetadata() {
        return java.util.Collections.unmodifiableMap(metadata);
    }

    public final String getMetadata(String name) {
        return metadata.get(name);
    }

    public final int getMetadataCount() {
        return metadata.size();
    }

    public final Iterable<String> getMetadataNames() {
        return java.util.Collections.unmodifiableSet(metadata.keySet());
    }

    public final void setMetadata(String name, String value) {
        if ((name != null) && !"".equals(name) && (value != null) && !"".equals(value)) {
            metadata.put(name, value);
        }
    }
}
