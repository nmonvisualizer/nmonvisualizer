package com.ibm.nmon.data;

public final class ProcessDataType extends DataType {
    private final Process process;

    public ProcessDataType(Process process, String... fields) {
        super(process.getTypeId(), process.toString(), fields);

        this.process = process;
    }

    public Process getProcess() {
        return process;
    }

    @Override
    public int hashCode() {
        return process.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        else if (o instanceof ProcessDataType) {
            return this.process.equals(((ProcessDataType) o).process);
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        // name is process.toString()
        return getName();
    }
}
