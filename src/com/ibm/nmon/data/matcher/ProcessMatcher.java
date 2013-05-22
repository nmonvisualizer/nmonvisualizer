package com.ibm.nmon.data.matcher;

import java.util.List;
import java.util.Map;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.Process;
import com.ibm.nmon.data.ProcessDataSet;
import com.ibm.nmon.util.DataHelper;

/**
 * Matches processes via {@link ProcessDataType}. This matcher matches either:
 * <ul>
 * <li>Aggregated processes when there is more than one process by the same name in a dataset</li>
 * <li>A single process instance when that process is the only one in the system</li>
 * </ul>
 * This class <em>does not </em> match a specific process by name or id.
 */
public final class ProcessMatcher implements TypeMatcher {
    public static final ProcessMatcher INSTANCE = new ProcessMatcher();

    private ProcessMatcher() {}

    @Override
    public List<DataType> getMatchingTypes(DataSet data) {
        List<DataType> types = new java.util.ArrayList<DataType>();

        Map<String, List<Process>> processNameToProcesses = DataHelper.getProcessesByName((ProcessDataSet) data, true);

        for (String processName : processNameToProcesses.keySet()) {
            List<Process> processes = processNameToProcesses.get(processName);

            // processes list contains either 1 item or the first item will be the aggregate process
            DataType type = ((ProcessDataSet) data).getType(processes.get(0));

            types.add(type);
        }

        return types;
    }

    @Override
    public String toString() {
        return "$PROCESSES";
    }
}
