package com.ibm.nmon.data;

import java.util.Set;

public abstract class ProcessDataSet extends DataSet {
    private final Set<Process> processes = new java.util.HashSet<Process>();

    public final void addProcess(Process process) {
        processes.add(process);
    }

    public final Iterable<Process> getProcesses() {
        return java.util.Collections.unmodifiableSet(processes);
    }

    public final int getProcessCount() {
        return processes.size();
    }

    public final ProcessDataType getType(Process process) {
        return (ProcessDataType) getType(process.getTypeId());
    }

    public String getTypeIdPrefix() {
        return "TOP";
    }

    public final Process changeStartTime(Process process, long newStartTime) {
        if (processes.contains(process)) {
            return process;
        }

        ProcessDataType type = getType(process);

        Process newProcess = new Process(process.getId(), newStartTime, process.getName());
        ProcessDataType newType = new ProcessDataType(newProcess, type.getFields().toArray(
                new String[type.getFieldCount()]));

        for (DataRecord record : getRecords()) {
            if (record.hasData(type)) {
                record.addData(newType, record.getData(type));
                record.removeData(type);
            }
        }

        removeType(type);
        addType(newType);

        processes.remove(process);
        processes.add(newProcess);

        return newProcess;
    }
}
