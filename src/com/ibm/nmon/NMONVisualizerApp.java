package com.ibm.nmon;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.util.Set;
import java.util.Map;
import java.util.TimeZone;

import java.util.Properties;

import org.slf4j.Logger;

import com.ibm.nmon.data.DataSetListener;
import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.NMONDataSet;
import com.ibm.nmon.data.SystemDataSet;

import com.ibm.nmon.parser.*;
import com.ibm.nmon.parser.gc.VerboseGCParser;
import com.ibm.nmon.interval.*;

import com.ibm.nmon.analysis.AnalysisRecord;

import com.ibm.nmon.util.ParserLog;
import com.ibm.nmon.util.TimeFormatCache;
import com.ibm.nmon.util.TimeZoneFactory;
import com.ibm.nmon.file.CombinedFileFilter;

/**
 * Main application base class responsible for parsing and managing {@link DataSet DataSets} and
 * {@link AnalysisRecord AnalysisRecords}; managing intervals, times and time zones; and managing
 * application level properties.
 * 
 * @see IntervalManager
 * @see NMONParser
 * @see VerboseGCParser
 */
public abstract class NMONVisualizerApp implements IntervalListener {
    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    private final NMONParser nmonParser;
    private final VerboseGCParser gcParser;
    private final IOStatParser iostatParser;
    private final JSONParser jsonParser;
    private final HATJParser hatJParser;
    private final ESXTopParser esxTopParser;

    // assume event order does not matter
    private final Set<DataSetListener> listeners;

    private final IntervalManager intervalManager = new IntervalManager();

    private TimeZone displayTimeZone;

    private final Map<SystemDataSet, AnalysisRecord> analysisRecords = new java.util.TreeMap<SystemDataSet, AnalysisRecord>();

    private long minSystemTime = 0;
    private long maxSystemTime = Long.MAX_VALUE;

    private final Properties properties = new Properties();
    protected final PropertyChangeSupport propertyChangeSupport;

    protected NMONVisualizerApp() {
        // ensure ParserLogHandler is instantiated before any parsers
        // since it creates the parent logger
        ParserLog.getInstance();

        nmonParser = new NMONParser();
        gcParser = new VerboseGCParser();
        iostatParser = new IOStatParser();
        jsonParser = new JSONParser();
        hatJParser = new HATJParser();
        esxTopParser = new ESXTopParser();

        TimeZone defaultTz = TimeZone.getDefault();

        // use the timezone names from TimeZoneFactory, if possible
        for (TimeZone timeZone : TimeZoneFactory.TIMEZONES) {
            long defaultOffset = defaultTz.getOffset(System.currentTimeMillis());

            if (timeZone.getOffset(System.currentTimeMillis()) == defaultOffset) {
                displayTimeZone = timeZone;
                break;
            }
        }

        if (displayTimeZone == null) {
            displayTimeZone = defaultTz;
        }

        TimeFormatCache.setTimeZone(displayTimeZone);

        listeners = new java.util.HashSet<DataSetListener>();
        propertyChangeSupport = new PropertyChangeSupport(this);

        intervalManager.addListener(this);

        setProperty("systemsNamedBy", "host");
    }

    /**
     * <p>
     * Parse the given file, if possible.
     * </p>
     * <p>
     * This method uses {@link SystemDataSet SystemDataSets} to store the parsed data. If multiple
     * files from the same host are parsed, the data is merged into a single data set. There will
     * never be multiple data sets stored by this class from the same host.
     * </p>
     * <p>
     * This time zone passed to this file should be the time zone where the data was
     * <em>collected</em> not where the application is running. All parsed files should line up to
     * same absolute epoch time. The {@link #setDisplayTimeZone(TimeZone) displayed time zone} will
     * control how this time is presented to end users.
     */
    public final void parse(String fileToParse, TimeZone timeZone) throws Exception {
        fileToParse = fileToParse.replace('\\', '/');

        // skipped already parsed files
        for (SystemDataSet systemData : analysisRecords.keySet()) {
            if (systemData.containsSourceFile(fileToParse)) {
                return;
            }
        }

        DataSet data = null;
        CombinedFileFilter filter = CombinedFileFilter.getInstance(false);

        if (filter.getNMONFileFilter().accept(fileToParse)) {
            data = nmonParser.parse(fileToParse, timeZone);
            String systemsNamedBy = getProperty("systemsNamedBy");

            if ("lpar".equals(systemsNamedBy)) {
                NMONDataSet nmonData = (NMONDataSet) data;

                if (nmonData.getMetadata("AIX") != null) {
                    String lparstat = nmonData.getSystemInfo("lparstat -i");

                    if (lparstat != null) {
                        int idx = lparstat.indexOf("Partition Name");

                        if (idx != -1) {
                            // some number of spaces before the colon
                            idx = lparstat.indexOf(": ", idx);

                            int end = lparstat.indexOf("\n", idx);

                            String partitionName = lparstat.substring(idx + 2, end);

                            nmonData.setMetadata("host", partitionName);
                        }
                    }
                }
            }
            else if ("run".equals(systemsNamedBy)) {
                NMONDataSet nmonData = (NMONDataSet) data;
                String runname = nmonData.getMetadata("runname");

                if (runname != null) {
                    nmonData.setMetadata("host", runname);
                }
            }
        }
        else if (filter.getGCFileFilter().accept(fileToParse)) {
            // GC data does not have a hostname or JVM name so get it before parsing
            String[] values = getDataForGCParse(fileToParse);

            if (values == null) {
                logger.info("skipping file '{}'", fileToParse);
                return;
            }
            else if (values.length < 2) {
                logger.error("need both hostname and JVM name to parse GC data, only {} provided",
                        java.util.Arrays.toString(values));
                return;
            }
            else {
                data = gcParser.parse(fileToParse, timeZone, values[0], values[1]);
            }
        }
        else if (filter.getIOStatFileFilter().accept(fileToParse)) {
            // IOStat data may have a hostname and time zone so get it after parsing
            data = iostatParser.parse(fileToParse, getDisplayTimeZone());

            String hostname = data.getHostname();
            boolean verifyDate = "AIX".equals(((com.ibm.nmon.data.BasicDataSet) data).getMetadata("OS"));

            // assume AIX, which also needs a parsed date
            if (hostname.equals(IOStatParser.DEFAULT_HOSTNAME) || verifyDate) {
                Object[] values = getDataForIOStatParse(fileToParse, hostname);

                if (values == null) {
                    logger.info("skipping file '{}'", fileToParse);
                    return;
                }

                hostname = (String) values[0];
                data.setHostname(hostname);

                long newDate = (Long) values[1];
                long defaultDate = IOStatParser.getDefaultDate();

                if (defaultDate != newDate) {
                    long start = System.nanoTime();

                    data.adjustTimes(newDate - defaultDate);

                    if (logger.isDebugEnabled()) {
                        logger.debug("data for '{}' times adjusted by {} hours in {} ms", new Object[] { fileToParse,
                                (newDate - defaultDate) / 3600.0 / 1000.0, System.nanoTime() - start });
                    }
                }
            }
        }
        else if (filter.getJSONFileFilter().accept(fileToParse)) {
            data = jsonParser.parse(fileToParse);
        }
        else if (filter.getHATJFileFilter().accept(fileToParse)) {
            data = hatJParser.parse(fileToParse);

            String hostname = data.getHostname();

            if (hostname.equals(HATJParser.DEFAULT_HOSTNAME)) {
                Object[] values = getDataForHATJParse(fileToParse, hostname);

                if (values == null) {
                    logger.info("skipping file '{}'", fileToParse);
                    return;
                }

                hostname = (String) values[0];
                data.setHostname(hostname);
            }
        }
        else if (filter.getESXTopFileFilter().accept(fileToParse)) {
            data = esxTopParser.parse(fileToParse);
        }
        else {
            throw new IllegalArgumentException("cannot parse " + fileToParse + ": unknown file type");
        }

        if (data.getRecordCount() == 0) {
            throw new IllegalArgumentException(fileToParse + " does not appear to contain any data");
        }

        // find an existing data set for the host
        SystemDataSet systemData = null;

        for (SystemDataSet toSearch : analysisRecords.keySet()) {
            if (toSearch.getHostname().equals(data.getHostname())) {
                systemData = toSearch;
                break;
            }
        }

        // create the data set if none exists
        if (systemData == null) {
            systemData = new SystemDataSet(data.getHostname());

            AnalysisRecord record = new AnalysisRecord(systemData);
            record.setInterval(intervalManager.getCurrentInterval());

            analysisRecords.put(systemData, record);
        }

        // add the parsed data to the system data set
        systemData.addData(fileToParse, data);

        recalculateMinAndMaxSystemTime();

        fireDataAdded(systemData);
    }

    protected String[] getDataForGCParse(String fileToParse) {
        // hostname and JVM name default to the file name
        int idx = fileToParse.lastIndexOf('/');

        if (idx != -1) {
            fileToParse = fileToParse.substring(idx + 1);
        }

        return new String[] { fileToParse, fileToParse };
    }

    protected Object[] getDataForIOStatParse(String fileToParse, String hostname) {
        // hostname, date
        return new Object[] { IOStatParser.DEFAULT_HOSTNAME, IOStatParser.getDefaultDate() };
    }

    protected Object[] getDataForHATJParse(String fileToParse, String hostname) {
        // hostname
        return new Object[] { HATJParser.DEFAULT_HOSTNAME };
    }

    // this is a separate function in order to allow subclasses (i.e. the gui) to run parsing in
    // another thread yet still fire the data added event in the main thread
    protected void fireDataAdded(DataSet data) {
        for (DataSetListener listener : listeners) {
            listener.dataAdded(data);
        }
    }

    public final TimeZone getDisplayTimeZone() {
        return displayTimeZone;
    }

    public final void setDisplayTimeZone(TimeZone displayTimeZone) {
        if (!this.displayTimeZone.equals(displayTimeZone)) {
            TimeZone old = this.displayTimeZone;

            this.displayTimeZone = displayTimeZone;

            TimeFormatCache.setTimeZone(displayTimeZone);
            propertyChangeSupport.firePropertyChange("timeZone", old, this.displayTimeZone);
        }
    }

    public final void removeDataSet(DataSet data) {
        if (analysisRecords.remove(data) != null) {
            recalculateMinAndMaxSystemTime();

            if (analysisRecords.isEmpty()) {
                for (DataSetListener listener : listeners) {
                    listener.dataCleared();
                }
            }
            else {
                for (DataSetListener listener : listeners) {
                    listener.dataRemoved(data);
                }
            }
        }
    }

    public final void updateDataSet(SystemDataSet data) {
        if (analysisRecords.remove(data) != null) {
            AnalysisRecord record = new AnalysisRecord(data);
            record.setInterval(intervalManager.getCurrentInterval());

            analysisRecords.put(data, record);

            recalculateMinAndMaxSystemTime();

            for (DataSetListener listener : listeners) {
                listener.dataChanged(data);
            }
        }
    }

    public final void clearDataSets() {
        minSystemTime = 0;
        maxSystemTime = Long.MAX_VALUE;

        TimeFormatCache.setDefaultIntervalRange(minSystemTime, maxSystemTime);
        intervalManager.setCurrentInterval(Interval.DEFAULT);

        for (DataSetListener listener : listeners) {
            listener.dataCleared();
        }

        // clear the records last to avoid errors when listeners still need access to analysis
        // records
        analysisRecords.clear();
    }

    public final Iterable<SystemDataSet> getDataSets() {
        return java.util.Collections.unmodifiableSet(analysisRecords.keySet());
    }

    public final int getDataSetCount() {
        return analysisRecords.size();
    }

    /**
     * @return the minimum time defined by any parsed DataSet or 0 if nothing has been parsed
     */
    public final long getMinSystemTime() {
        return minSystemTime;
    }

    /**
     * @return the maximum time defined by any parsed DataSet or <code>Long.MAX_VALUE</code> if
     *         nothing has been parsed
     */
    public final long getMaxSystemTime() {
        return maxSystemTime;
    }

    public final IntervalManager getIntervalManager() {
        return intervalManager;
    }

    public final AnalysisRecord getAnalysis(DataSet data) {
        return analysisRecords.get(data);
    }

    public final String getProperty(String name) {
        return properties.getProperty(name);
    }

    public final boolean getBooleanProperty(String name) {
        return Boolean.parseBoolean(properties.getProperty(name));
    }

    /**
     * Set an application level property and fire a {@link PropertyChangeEvent} for the associated
     * property. No attempt is made to see if the property actually changed.
     */
    public final void setProperty(String name, String value) {
        String old = properties.getProperty(name);

        properties.setProperty(name, value);

        propertyChangeSupport.firePropertyChange(name, old, value);
    }

    public final void setProperty(String name, int value) {
        String temp = properties.getProperty(name);

        int old = -1;

        if (temp != null) {
            old = Integer.parseInt(temp);
        }

        properties.setProperty(name, Integer.toString(value));

        propertyChangeSupport.firePropertyChange(name, old, value);
    }

    public final void setProperty(String name, boolean value) {
        boolean old = getBooleanProperty(name);

        properties.setProperty(name, Boolean.toString(value));

        propertyChangeSupport.firePropertyChange(name, old, value);
    }

    // update the start and end times when new DataSets are added
    // this may change the meaning of Interval.DEFAULT so update that if necessary
    private void recalculateMinAndMaxSystemTime() {
        if (analysisRecords.size() > 0) {
            long minStart = Long.MAX_VALUE;
            long maxEnd = Long.MIN_VALUE;

            for (DataSet data : analysisRecords.keySet()) {
                if (data.getStartTime() < minStart) {
                    minStart = data.getStartTime();
                }

                if (data.getEndTime() > maxEnd) {
                    maxEnd = data.getEndTime();
                }
            }

            boolean update = false;

            if (minStart != minSystemTime) {
                minSystemTime = minStart;
                update = true;
            }

            if (maxEnd != maxSystemTime) {
                maxSystemTime = maxEnd;
                update = true;
            }

            if (update && intervalManager.getCurrentInterval().equals(Interval.DEFAULT)) {
                TimeFormatCache.setDefaultIntervalRange(minSystemTime, maxSystemTime);
                intervalManager.setCurrentInterval(Interval.DEFAULT);
            }
        }
    }

    @Override
    public void intervalAdded(Interval interval) {}

    @Override
    public void intervalRemoved(Interval interval) {}

    @Override
    public void intervalsCleared() {
        // assume clearing also sets current interval back to DEFAULT
    }

    @Override
    public void currentIntervalChanged(Interval interval) {
        for (AnalysisRecord record : analysisRecords.values()) {
            record.setInterval(interval);
        }
    }

    @Override
    public void intervalRenamed(Interval interval) {}

    public void addDataSetListener(DataSetListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }
    }

    public void removeDataSetListener(DataSetListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }
}
