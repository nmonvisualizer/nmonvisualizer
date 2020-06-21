package com.ibm.nmon.data;

/**
 * <p>
 * A class that stores data for a single operating system process. Processes have an id, name,
 * command line and start &amp; end times.
 * </p>
 * 
 * <p>
 * The process id can be -1, which indicates that the process denotes an aggregated view of all
 * processes on a system with the same name.
 * </p>
 */
public final class Process implements Comparable<Process> {
    // note Process with id of -1 is used to represent an aggregate of all processes with the given
    // name; it is treated differently in terms of id, equals and hashcode
    private final int id;
    private final String name;
    private String commandLine;
    private final long startTime;
    private long endTime = Long.MAX_VALUE;

    private final String typeId;

    public Process(int id, long startTime, String name) {
        this(id, startTime, name, "TOP");
    }

    public Process(int id, long startTime, String name, String typeName) {
        this.id = id;
        this.startTime = startTime;
        this.name = name;
        this.commandLine = "";

        if (id == -1) {
            this.typeId = typeName + "-ALL-" + name;
        }
        else {
            this.typeId = typeName + "-" + id + '@' + startTime;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setCommandLine(String commandLine) {
        if (commandLine == null) {
            this.commandLine = "";
        }
        else {
            this.commandLine = commandLine;
        }
    }

    public String getCommandLine() {
        return commandLine;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        if (startTime > endTime) {
            throw new IllegalArgumentException("end time must be greater than start time");
        }

        this.endTime = endTime;
    }

    @Override
    public String toString() {
        if (id == -1) {
            return name;
        }
        else {
            return name + " (" + id + ')';
        }
    }

    // default access for DataSets and DataTypes
    String getTypeId() {
        return typeId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        else if (obj instanceof Process) {
            Process p = (Process) obj;

            if (id == -1) {
                return this.name.equals(p.name);
            }
            else {
                return (this.id == p.id) && (this.startTime == p.startTime);
            }
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
       return typeId.hashCode();
    }

    @Override
    public int compareTo(Process p) {
        int compare = this.name.compareTo(p.name);

        if (compare == 0) {
            return this.id - p.id;
        }
        else {
            return compare;
        }
    }
}
