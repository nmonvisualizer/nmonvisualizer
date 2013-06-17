package com.ibm.nmon.interval;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;

import com.ibm.nmon.util.DataHelper;
import com.ibm.nmon.util.TimeFormatCache;
import com.ibm.nmon.util.TimeHelper;

/**
 * A manager implementation for {@link Interval intervals}. Maintains a list of multiple intervals
 * along with a 'current' interval that is being analyzed. On changes, the manager will fire
 * {@link IntervalListener} events.
 */
public final class IntervalManager {
    private final Set<Interval> intervals;

    private Interval currentInterval;

    private List<IntervalListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<IntervalListener>();

    public IntervalManager() {
        intervals = new java.util.TreeSet<Interval>();
        currentInterval = Interval.DEFAULT;
    }

    public Interval getCurrentInterval() {
        return currentInterval;
    }

    public void setCurrentInterval(Interval interval) {
        if (intervals.contains(interval)) {
            if (!currentInterval.equals(interval)) {
                currentInterval = interval;

                for (IntervalListener listener : listeners) {
                    listener.currentIntervalChanged(interval);
                }
            }
        }
        else if (Interval.DEFAULT.equals(interval)) {
            // call the listeners even if the current interval is already Interval.DEFAULT
            // this is needed in case data is added that redefines what the default interval time
            // span actually is
            // see com.ibm.nmon.NMONVisualizerApp.recalculateMinAndMaxSystemTime()
            currentInterval = Interval.DEFAULT;

            for (IntervalListener listener : listeners) {
                listener.currentIntervalChanged(interval);
            }
        }
    }

    public Iterable<Interval> getIntervals() {
        return java.util.Collections.unmodifiableSet(intervals);
    }

    public int getIntervalCount() {
        return intervals.size();
    }

    public boolean addInterval(Interval interval) {
        if (Interval.DEFAULT.equals(interval)) {
            return false;
        }
        else if (intervals.add(interval)) {
            for (IntervalListener listener : listeners) {
                listener.intervalAdded(interval);
            }

            return true;
        }
        else {
            return false;
        }
    }

    public boolean removeInterval(Interval interval) {
        if (intervals.remove(interval)) {
            for (IntervalListener listener : listeners) {
                listener.intervalRemoved(interval);
            }

            if (currentInterval.equals(interval)) {
                setCurrentInterval(Interval.DEFAULT);
            }

            return true;
        }
        else {
            return false;
        }
    }

    public void clearIntervals() {
        intervals.clear();

        for (IntervalListener listener : listeners) {
            listener.intervalsCleared();
        }

        setCurrentInterval(Interval.DEFAULT);
    }

    public void renameInterval(Interval interval, String newName) {
        if (intervals.contains(interval)) {
            if (newName == null) {
                newName = "";
            }

            if (!interval.getName().equals(newName)) {
                interval.setName(newName);
                TimeFormatCache.renameInterval(interval);

                for (IntervalListener listener : listeners) {
                    listener.intervalRenamed(interval);
                }
            }
        }
    }

    public void addListener(IntervalListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(IntervalListener listener) {
        listeners.remove(listener);
    }

    private static final Pattern DATA_SPLITTER = Pattern.compile("\"?,\"?");

    public void loadFromFile(File file, long offset) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new java.io.FileReader(file));

            String line = null;
            Interval interval = null;

            while ((line = reader.readLine()) != null) {
                String[] data = DATA_SPLITTER.split(line);

                String name = "";
                String start = "";
                String end = "";

                switch (data.length) {
                case 0:
                    continue;
                case 1:
                    continue;
                case 2: {
                    start = data[0];
                    end = data[1];
                    break;
                }
                case 3: {
                    name = DataHelper.newString(data[0]);
                    start = data[1];
                    end = data[2];
                    break;
                }
                default:
                    continue;
                }

                long startTime = 0;
                long endTime = 0;

                try {
                    startTime = TimeHelper.TIMESTAMP_FORMAT_ISO.parse(start).getTime();
                }
                catch (ParseException pe) {
                    try {
                        startTime = Long.parseLong(start) + offset;
                    }
                    catch (NumberFormatException nfe) {
                        continue;
                    }
                }

                try {
                    endTime = TimeHelper.TIMESTAMP_FORMAT_ISO.parse(end).getTime();
                }
                catch (ParseException pe) {
                    try {
                        endTime = Long.parseLong(end) + offset;
                    }
                    catch (NumberFormatException nfe) {
                        continue;
                    }
                }

                interval = new Interval(startTime, endTime);
                interval.setName(name);

                addInterval(interval);
            }

            if (interval != null) {
                setCurrentInterval(interval);
            }
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ioe2) {
                    // ignore
                }
            }
        }
    }

    public void saveToFile(File file, long offset) throws IOException {
        FileWriter writer = null;

        try {
            writer = new FileWriter(file);

            for (Interval interval : getIntervals()) {
                long start = interval.getStart() - offset;
                long end = interval.getEnd() - offset;

                writer.write(interval.getName());
                writer.write(':');

                if (offset == 0) { // absolute time
                    writer.write(TimeHelper.TIMESTAMP_FORMAT_ISO.format(new Date(start)));
                    writer.write(':');
                    writer.write(TimeHelper.TIMESTAMP_FORMAT_ISO.format(new Date(end)));
                }
                else { // relative time
                    writer.write(Long.toString(start));
                    writer.write(':');
                    writer.write(Long.toString(end));
                }

                writer.write('\n');
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (IOException ioe2) {
                    // ignore
                }
            }
        }
    }
}
