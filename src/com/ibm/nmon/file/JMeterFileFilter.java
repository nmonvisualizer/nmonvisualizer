package com.ibm.nmon.file;

public final class JMeterFileFilter extends BaseFileFilter {
    public boolean accept(String pathname) {
        String name = pathname.toLowerCase();
        return name.endsWith(".csv") && (name.contains("jmeter") || name.contains("aggregate"));
    }

    public JMeterFileFilter() {}
}
