package com.ibm.nmon.interval;

/**
 * Defines a time interval with a given start and end value, measured in milliseconds.
 */
public final class Interval implements Comparable<Interval> {
    /**
     * Interval to indicate all available data. The start and end values <em>should</em> not be used
     * directly. Instead, this interval should be used as a marker to indicate that the start and
     * end values should be looked up elsewhere.
     */
    public static final Interval DEFAULT = new Interval(0, Long.MAX_VALUE);

    private String name;

    private final long start;
    private final long end;

    public Interval(long start, long end) {
        this.name = "";
        this.start = start;
        this.end = end;

        validate();
    }

    private void validate() {
        if (start >= end) {
            throw new IllegalArgumentException("end" + " must be greater than " + "start");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            this.name = "";
        }
        else {
            this.name = name;
        }
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getDuration() {
        return end - start;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() == Interval.class) {
            Interval i = (Interval) obj;

            return (this.start == i.start) && (this.end == i.end);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int) (start ^ (start >>> 32)) * 31 + (int) (end ^ (end >>> 32));
    }

    @Override
    public int compareTo(Interval i) {
        if (this.start == i.start) {
            if (this.end == i.end) {
                return 0;
            }
            else {
                return this.end > i.end ? 1 : -1;
            }
        }
        else {
            return this.start > i.start ? 1 : -1;
        }
    }
}
