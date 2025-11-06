package com.ibm.nmon.util;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.ProcessDataSet;
import com.ibm.nmon.data.Process;
import com.ibm.nmon.data.ProcessDataType;

/**
 * Utility methods for working with DataSets and DataTypes.
 */
public final class DataHelper {
    public static Map<String, List<Process>> getProcessesByName(ProcessDataSet data, boolean sorted) {
        /**
         * Collect all the processes in a file and group them by name.
         * 
         * @return a Map of process names to a list of Processes. The List will never be empty or <code>null</code>.
         */
        Map<String, List<Process>> processNameToProcesses = null;

        if (sorted) {
            processNameToProcesses = new java.util.TreeMap<String, List<Process>>();
        }
        else {
            processNameToProcesses = new java.util.HashMap<String, List<Process>>();
        }

        // collect all the process names and map them to a Process
        for (Process process : data.getProcesses()) {
            List<Process> processes = processNameToProcesses.get(process.getName());

            if (processes == null) {
                processes = new java.util.ArrayList<Process>();
                processNameToProcesses.put(process.getName(), processes);
            }

            processes.add(process);
        }

        if (sorted) {
            for (List<Process> processes : processNameToProcesses.values()) {
                // note that this puts the aggregated process data (with pid -1) at the beginning of
                // the list
                java.util.Collections.sort(processes);
            }
        }

        return processNameToProcesses;
    }

    public static void aggregateProcessData(ProcessDataSet data, Logger logger) {
        long start = System.nanoTime();
        Map<String, List<Process>> processNameToProcesses = DataHelper.getProcessesByName(data, false);

        List<Process> allProcesses = new ArrayList<>();
        for (List<Process> processes : processNameToProcesses.values()) {
            allProcesses.addAll(processes);
        }
        if (allProcesses.size() > 0) {
            aggregateProcesses(data, allProcesses, "ALLPROCESSES", logger);
        }

        for (List<Process> processes : processNameToProcesses.values()) {
            if (processes.size() > 1) {
                aggregateProcesses(data, processes, processes.get(0).getName(), logger);
            }
            allProcesses.addAll(processes);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Aggregated process data for {} in {}ms ", data, (System.nanoTime() - start) / 1000000.0d);
        }
    }

    private static void aggregateProcesses(ProcessDataSet data, List<Process> processes, String name, Logger logger) {
        long start = System.nanoTime();
        long earliestStart = Long.MAX_VALUE;

        for (Process process : processes) {
            if (process.getStartTime() < earliestStart) {
                earliestStart = process.getStartTime();
            }
        }

        // assume first process is representative and fields do not change
        // note that this aggregates processes with the same name but different commands lines,
        // i.e. different Java web servers
        ProcessDataType processType = data.getType(processes.get(0));

        int fieldCount = processType.getFieldCount() + 1;

        // copy the fields from the Process
        // add a Count field to count the number of processes running at each time period
        String[] fields = new String[fieldCount];

        for (int i = 0; i < fieldCount - 1; i++) {
            fields[i] = processType.getField(i);
        }

        fields[fieldCount - 1] = "Count";

        Process aggregate = new Process(-1, earliestStart, name);
        aggregate.setCommandLine("all " + name + " processes");
        ProcessDataType aggregateType = new ProcessDataType(aggregate, fields);

        data.addProcess(aggregate);
        data.addType(aggregateType);

        // for every record in the file, sum up all the data for each process and add the aggregated
        // data to the record
        for (DataRecord record : data.getRecords()) {
            double[] totals = new double[fieldCount];

            java.util.Arrays.fill(totals, 0);

            // does any process have data at this time?
            boolean valid = false;

            for (Process process : processes) {
                processType = data.getType(process);

                if (record.hasData(processType)) {
                    valid = true;

                    int n = 0;

                    for (String field : processType.getFields()) {
                        totals[n++] += record.getData(processType, field);
                    }

                    // process count
                    ++totals[n];
                }
            }

            if (valid) {
                record.addData(aggregateType, totals);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Aggregated process data for {} in {}ms ", name, (System.nanoTime() - start) / 1000000.0d);
        }
    }

    /**
     * Does the given disk name denote a disk or a partition?
     * 
     * @return true if the name is a disk
     */
    public static boolean isNotPartition(String diskName) {
        // assume disk names are like sdx, sdx1, sdx2, etc on Linux
        // disks do not have digits; paritions do
        boolean hdisk = diskName.startsWith("hdisk");

        if ((diskName.startsWith("sd") || (diskName.startsWith("hd") && !hdisk) || diskName.startsWith("vd")
                || diskName.startsWith("fio") || diskName.startsWith("xvd") || diskName.startsWith("dasd"))
                && !Character.isDigit(diskName.charAt(diskName.length() - 1))) {
            return true;

        }
        // hdisks on AIX are physical
        else if (hdisk) {
            return true;
        }
        // device mappers are always 'disks'
        // but Linux seems to report both dm AND sdx traffic so exclude them
        else {
            return false;
        }
    }

    /**
     * Get a sorted list of network interfaces, assuming field names in the DataType are like 'eth0-read-KB/s'.
     */
    public static SortedSet<String> getInterfaces(DataType type) {
        SortedSet<String> ifaces = new java.util.TreeSet<String>();

        for (String field : type.getFields()) {
            int idx = field.indexOf("-read-KB/s");

            if (idx != -1) {
                ifaces.add(field.substring(0, idx));
            }
            else {
                idx = field.indexOf("-write-KB/s");
                if (idx != -1) {
                    ifaces.add(field.substring(0, idx));
                }
            }
        }

        return ifaces;
    }

    private static final boolean IS_IBM_JVM = System.getProperty("java.vm.vendor").startsWith("IBM");

    /**
     * Create a new String. This method is used to save memory usage for Strings that are created via substring.
     */
    public static String newString(String original) {
        // new String to avoid memory leaks when the element is stored
        // IBM JVMs do not create a new char[] on copy, so do it manually
        // this is inefficient because the array is double copied, but that's better
        // than leaving the entire string in memory
        if (IS_IBM_JVM) {
            return new String(original.trim().toCharArray()).intern();
        }
        else {
            // Oracle JVM's String implementation is always the correct size
            return original.trim().intern();
        }
    }

    private DataHelper() {}
}
