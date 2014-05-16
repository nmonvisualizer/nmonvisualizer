package com.ibm.nmon.gui.chart.annotate;

import org.jfree.chart.plot.ValueMarker;

/**
 * Trivial subclass so that Domain versus Range axis markers can be separated and handled
 * differently.
 * 
 * @see ValueMarker
 */
public final class RangeValueMarker extends ValueMarker {
    private static final long serialVersionUID = -3459747427167794553L;

    public RangeValueMarker(double value) {
        super(value);
    }
}
