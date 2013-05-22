package com.ibm.nmon.data;

import org.slf4j.Logger;

import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

/**
 * A DataSet designed to hold data for a single 'system' or host across a number of parsed files.
 * Parsed files are identified by the {@link DataSet#getStartTime() start time} of the file, so
 * these times should be unique.
 */
public final class SystemDataSet extends ProcessDataSet {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SystemDataSet.class);

    private final TreeMap<Long, Map<String, String>> systemInfo = new TreeMap<Long, Map<String, String>>();
    private final TreeMap<Long, Map<String, String>> metadata = new TreeMap<Long, Map<String, String>>();

    private final TreeMap<Long, String> sourceFiles = new java.util.TreeMap<Long, String>();

    private final String hostname;

    public SystemDataSet(String hostname) {
        if ((hostname == null) || "".equals(hostname)) {
            throw new IllegalArgumentException("hostname cannot be " + "null");
        }

        this.hostname = hostname;
    }

    public final String getHostname() {
        return hostname;
    }

    @Override
    public void setHostname(String hostname) {
        throw new UnsupportedOperationException("hostname cannot be " + "changed");
    }

    public String getSourceFile() {
        int size = sourceFiles.size();

        if (size == 0) {
            return "no data";
        }
        else if (size == 1) {
            return sourceFiles.get(0);
        }
        else {
            return size + " files";
        }
    }

    public Iterable<String> getSourceFiles() {
        return java.util.Collections.unmodifiableCollection(sourceFiles.values());
    }

    public String getSourceFile(long time) {
        return sourceFiles.get(time);
    }

    public int getSourceFileCount() {
        return sourceFiles.size();
    }

    public Iterable<Long> getSourceFileTimes() {
        return java.util.Collections.unmodifiableSet(sourceFiles.keySet());
    }

    public boolean containsSourceFile(String sourceFile) {
        return sourceFiles.values().contains(sourceFile);
    }

    public void addData(SystemDataSet newData) {
        long startT = System.nanoTime();

        merge(newData);

        sourceFiles.putAll(newData.sourceFiles);
        metadata.putAll(newData.metadata);
        systemInfo.putAll(newData.systemInfo);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("data for {} consolidated into data for {} in {}ms ", new Object[] { newData, getHostname(),
                    (System.nanoTime() - startT) / 1000000.0d });
        }
    }

    public void addData(String sourceFile, DataSet newData) {
        long startT = System.nanoTime();

        if ((sourceFile == null) || "".equals(sourceFile)) {
            throw new IllegalArgumentException("source file cannot be null");
        }

        merge(newData);

        long start = newData.getStartTime();

        sourceFiles.put(start, sourceFile);

        if (newData.getClass().equals(BasicDataSet.class)) {
            metadata.put(start, ((BasicDataSet) newData).getMetadata());
        }
        else if (newData.getClass().equals(NMONDataSet.class)) {
            NMONDataSet nmonData = (NMONDataSet) newData;

            systemInfo.put(start, nmonData.getSystemInfo());
            metadata.put(start, nmonData.getMetadata());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("data for {} consolidated into data for {} in {}ms ", new Object[] { sourceFile,
                    getHostname(), (System.nanoTime() - startT) / 1000000.0d });
        }
    }

    private void merge(DataSet newData) {
        if (newData == null) {
            throw new IllegalArgumentException("DataSet cannot be null");
        }

        if (newData.getRecordCount() == 0) {
            return;
        }

        Set<Long> combinedTimes = null;

        // minor optimization - if this data set is empty, only addNewType will be called below and
        // combinedTimes will not be needed but initialize it here to avoid doing that once per type
        // when it is needed
        if (getRecordCount() != 0) {
            combinedTimes = new java.util.HashSet<Long>(getTimes());
            combinedTimes.addAll(newData.getTimes());
        }

        for (DataType newType : newData.getTypes()) {
            if (newType instanceof ProcessDataType) {
                // handle processes differently since the the start time is calculated for each file
                // processed
                // assume a process with the same name and pid is the same for merging purposes
                ProcessDataType newProcessType = (ProcessDataType) newType;
                Process newProcess = newProcessType.getProcess();
                Process existingProcess = null;

                for (Process toSearch : getProcesses()) {
                    if (newProcess.getName().equals(toSearch.getName()) && (newProcess.getId() == toSearch.getId())) {
                        existingProcess = toSearch;
                        break;
                    }
                }

                if (existingProcess != null) {
                    addProcessData(newData, newProcessType, existingProcess);
                }
                else {
                    addProcess(newProcess);
                    addType(newProcessType);
                    addDataForType(newData, newProcessType);
                }
            }
            else { // not a process
                if (!containsType(newType.getId())) {
                    // new type - add it to the data
                    addType(newType);
                    addDataForType(newData, newType);
                }
                else {

                    mergeDataForType(newData, newType, combinedTimes);
                }
            }
        }
    }

    private void addDataForType(DataSet data, DataType type) {
        long start = System.nanoTime();
        int n = 0;

        for (DataRecord newRecord : data.getRecords()) {
            long time = newRecord.getTime();
            DataRecord recordToUpdate = getRecord(time);

            if (recordToUpdate == null) {
                recordToUpdate = new DataRecord(time, newRecord.getTimestamp());
                addRecord(recordToUpdate);
            }

            if (recordToUpdate.hasData(type)) {
                LOGGER.warn("not overwriting existing {} data at time {}", type, newRecord.getTimestamp());
            }
            else if (newRecord.hasData(type)) {
                recordToUpdate.addData(type, newRecord.getData(type));
                ++n;
            }
            else {
                // dangerous since this can potentially log a huge amount for process data
                // if (LOGGER.isTraceEnabled()) {
                // LOGGER.trace("no data for {} at time {}", type.id,
                // existingRecord.getTimestamp()); }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{} of {} data records for {} {} added in {}ms ", new Object[] { n, data.getRecordCount(),
                    data, type, (System.nanoTime() - start) / 1000000.0d });
        }
    }

    private void addProcessData(DataSet data, ProcessDataType processType, Process existingProcess) {
        long start = System.nanoTime();
        int n = 0;

        Process newProcess = processType.getProcess();

        long newStartTime = Math.min(newProcess.getStartTime(), existingProcess.getStartTime());

        Process updatedProcess = changeStartTime(existingProcess, newStartTime);
        ProcessDataType updatedProcessType = getType(updatedProcess);

        for (DataRecord newRecord : data.getRecords()) {
            long time = newRecord.getTime();
            DataRecord recordToUpdate = getRecord(time);

            if (recordToUpdate == null) {
                recordToUpdate = new DataRecord(time, newRecord.getTimestamp());
                addRecord(recordToUpdate);
            }

            if (recordToUpdate.hasData(updatedProcessType)) {
                LOGGER.warn("not overwriting existing {} data at time {}", updatedProcessType, newRecord.getTimestamp());
            }
            else if (newRecord.hasData(processType)) {
                // note processType since the new records have not been updated
                recordToUpdate.addData(updatedProcessType, newRecord.getData(processType));
                ++n;
            }
            else {
                // dangerous since this can potentially log a huge amount
                // if (LOGGER.isTraceEnabled()) {
                // LOGGER.trace("no data for {} at time {}", type.id,
                // existingRecord.getTimestamp()); }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{} of {} process data records for {} {} added in {}ms ",
                    new Object[] { n, data.getRecordCount(), data, updatedProcessType,
                            (System.nanoTime() - start) / 1000000.0d });
        }
    }

    private void mergeDataForType(DataSet newData, DataType newType, Set<Long> combinedTimes) {
        DataType existingType = getType(newType.getId());

        // union of the fields ...
        Set<String> combinedFields = new java.util.TreeSet<String>(existingType.getFields());
        combinedFields.addAll(newType.getFields());

        // no new fields in either data set
        // just add the records without overwriting the existing
        if ((combinedFields.size() == existingType.getFieldCount())
                && (combinedFields.size() == newType.getFieldCount())) {
            addDataForType(newData, newType);
            return;
        }
        // otherwise, actually merge the data

        long start = System.nanoTime();

        DataType combinedType = null;

        if (existingType instanceof SubDataType) {
            SubDataType subType = (SubDataType) existingType;
            combinedType = new SubDataType(subType.getPrimaryId(), subType.getSubId(), existingType.getName(),
                    combinedFields.toArray(new String[0]));
        }
        else {
            combinedType = new DataType(existingType.getId(), existingType.getName(),
                    combinedFields.toArray(new String[0]));
        }

        // data set now only contains the combined type
        removeType(existingType);
        addType(combinedType);

        int fieldCount = combinedFields.size();

        // for every record, re-add the field values when available
        for (long time : combinedTimes) {
            DataRecord existingRecord = getRecord(time);
            DataRecord newRecord = newData.getRecord(time);

            boolean hasExistingData = (existingRecord != null) && existingRecord.hasData(existingType);
            boolean hasNewData = (newRecord != null) && newRecord.hasData(newType);

            if (hasExistingData || hasNewData) {
                double[] combinedData = new double[fieldCount];
                int n = 0;

                for (String field : combinedFields) {
                    if (newType.hasField(field)) {
                        // existingType has the same field and has data for this time.
                        // Use that data and do not overwrite with new data
                        if (hasExistingData && existingType.hasField(field)) {
                            combinedData[n] = existingRecord.getData(existingType, field);

                            LOGGER.warn("not overwriting existing {} data at time {}", newType,
                                    existingRecord.getTimestamp());
                        }
                        else if (hasNewData) {
                            combinedData[n] = newRecord.getData(newType, field);
                        }
                        else {
                            combinedData[n] = Double.NaN;
                        }
                    }
                    else {
                        if (hasExistingData) {
                            combinedData[n] = existingRecord.getData(existingType, field);
                        }
                        else {
                            combinedData[n] = Double.NaN;
                        }
                    }

                    ++n;
                }

                if (existingRecord == null) {
                    DataRecord combinedRecord = new DataRecord(newRecord.getTime(), newRecord.getTimestamp());
                    combinedRecord.addData(combinedType, combinedData);
                    addRecord(combinedRecord);
                }
                else {
                    existingRecord.removeData(existingType);
                    existingRecord.addData(combinedType, combinedData);
                }
            }
            // else no data at this time from either new or existing,
            // just leave the record empty for this type
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("data records for {} {} merged in {}ms ",
                    new Object[] { newData, combinedType, (System.nanoTime() - start) / 1000000.0d });
        }
    }

    public Iterable<Long> getSystemInfoTimes() {
        return java.util.Collections.unmodifiableSet(systemInfo.keySet());
    }

    public Map<String, String> getSystemInfo(long time) {
        Map<String, String> toReturn = systemInfo.get(time);

        if (toReturn == null) {
            return java.util.Collections.emptyMap();
        }
        else {
            return java.util.Collections.unmodifiableMap(toReturn);
        }
    }

    public int getSystemInfoCount() {
        return systemInfo.size();
    }

    public Iterable<Long> getMetadataTimes() {
        return java.util.Collections.unmodifiableSet(metadata.keySet());
    }

    public Map<String, String> getMetadata(long time) {
        Map<String, String> toReturn = metadata.get(time);

        if (toReturn == null) {
            return java.util.Collections.emptyMap();
        }
        else {
            return java.util.Collections.unmodifiableMap(toReturn);
        }
    }

    public int getMetadataCount() {
        return metadata.size();
    }
}
