package com.ibm.nmon.util;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import java.util.Calendar;

import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.ProcessDataSet;

import com.ibm.nmon.data.Process;

/**
 * Utility methods for working with DataSets and DataTypes.
 */
public final class DataHelper {
    public static Map<String, List<Process>> getProcessesByName(ProcessDataSet data, boolean sorted) {
        /**
         * Collect all the processes in a file and group them by name.
         * 
         * @return a Map of process names to a list of Processes. The List will never be empty or
         *         <code>null</code>.
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

    /**
     * Does the given disk name denote a disk or a partition?
     * 
     * @return true if the name is a disk
     */
    public static boolean isNotPartition(String diskName) {
        // assume disk names are like sdx, sdx1, sdx2, etc on Linux
        // disks do not have digits; paritions do
        boolean hdisk = diskName.startsWith("hdisk");

        if ((diskName.startsWith("sd") || (diskName.startsWith("hd") && !hdisk))
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
     * Get a sorted list of network interfaces, assuming field names in the DataType are like
     * 'eth0-read-KB/s'.
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
     * Create a new String. This method is used to save memory usage for Strings that are created
     * via substring.
     */
    public static String newString(String original) {
        // new String to avoid memory leaks when the element is stored
        // IBM JVMs do not create a new char[] on copy, so do it manually
        // this is inefficient because the array is double copied, but that's better
        // than leaving the entire string in memory
        if (IS_IBM_JVM) {
            return new String(original.trim().toCharArray());
        }
        else {
            // Sun JVM's copy constructor creates a correct sized array
            // without an extra array copy
            return new String(original.trim());
        }
    }

    public static long dayFromDatetime(long datetime) {
        Calendar cal = new java.util.GregorianCalendar();
        cal.setTimeInMillis(datetime);

        // today
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime().getTime();
    }

    public static long today() {
        return dayFromDatetime(System.currentTimeMillis());
    }

    private DataHelper() {}
}
