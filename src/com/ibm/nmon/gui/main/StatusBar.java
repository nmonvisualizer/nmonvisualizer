package com.ibm.nmon.gui.main;

import javax.swing.JLabel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import javax.swing.SwingConstants;

import java.awt.Font;

import com.ibm.nmon.data.DataSetListener;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.SystemDataSet;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.tree.TreePathParser;

import com.ibm.nmon.interval.IntervalManager;
import com.ibm.nmon.interval.IntervalListener;
import com.ibm.nmon.interval.Interval;

/**
 * Optionally displayed status bar that displays the currently displayed chart's processed source
 * filename(s). The filenames displayed can changed based on the current <code>Interval</code>.
 * 
 * @see SystemDataSet#getSourceFiles()
 */
final class StatusBar extends JLabel implements PropertyChangeListener, TreeSelectionListener, IntervalListener,
        DataSetListener {
    private static final long serialVersionUID = -6249775876913359902L;

    private static final String NO_SOURCE_FILE = "<NONE>";

    private final IntervalManager intervalManager;

    private SystemDataSet currentData = null;

    public StatusBar(NMONVisualizerGui gui) {
        super();

        Font font = Styles.LABEL.deriveFont(Font.PLAIN);
        
        setText(NO_SOURCE_FILE);
        setFont(font);
        setForeground(new java.awt.Color(0x333333));
        setHorizontalAlignment(SwingConstants.CENTER);
        setBackground(new java.awt.Color(248, 248, 248));
        setOpaque(true);
        setBorder(Styles.createTopLineBorder(this));
        setVisible(gui.getBooleanProperty("showStatusBar"));

        intervalManager = gui.getIntervalManager();
        intervalManager.addListener(this);
        gui.addPropertyChangeListener("showStatusBar", this);
        gui.addDataSetListener(this);
    }

    private void updateStatus(Interval currentInterval) {
        if (!isVisible()) {
            return;
        }

        if (currentData == null) {
            setText(NO_SOURCE_FILE);
            setToolTipText(null);

            return;
        }

        StringBuilder builder = new StringBuilder(128);

        for (Interval interval : currentData.getSourceFileIntervals()) {
            if ((interval.getStart() <= currentInterval.getEnd()) && (interval.getEnd() >= currentInterval.getStart())) {
                builder.append(currentData.getSourceFile(interval));
                // builder.append(new File(sourceFile).getName());
                builder.append(" + ");
            }
        }

        if (builder.length() > 3) {
            String text = builder.substring(0, builder.length() - 3);

            setText(text);
            setToolTipText(text);
        }
        else {
            setText(NO_SOURCE_FILE);
            setToolTipText(null);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        setVisible((Boolean) evt.getNewValue());

        updateStatus(intervalManager.getCurrentInterval());
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        pathParser.parse(e.getPath());
    }

    public void intervalAdded(Interval interval) {}

    public void intervalRemoved(Interval interval) {}

    public void intervalsCleared() {
        currentIntervalChanged(intervalManager.getCurrentInterval());
    }

    public void currentIntervalChanged(Interval interval) {
        updateStatus(interval);
    }

    public void intervalRenamed(Interval interval) {}

    @Override
    public void dataAdded(DataSet data) {
        if (data.equals(currentData)) {
            updateStatus(intervalManager.getCurrentInterval());
        }
    }

    @Override
    public void dataRemoved(DataSet data) {
        dataAdded(data);
    }

    @Override
    public void dataChanged(DataSet data) {
        dataAdded(data);
    }

    @Override
    public void dataCleared() {}

    // continue to track current DataSet but do not update status if the status bar is not showing
    private final TreePathParser pathParser = new TreePathParser() {
        protected void onNullPath() {
            onDataSetPath(null);
        }

        protected void onRootPath() {
            onDataSetPath(null);
        }

        protected void onDataSetPath(DataSet data) {
            if (((data == null) && (currentData != null)) || ((data != null) && !data.equals(currentData))) {
                currentData = (SystemDataSet) data;

                updateStatus(intervalManager.getCurrentInterval());
            }
        }

        protected void onTypePath(DataSet data, com.ibm.nmon.data.DataType type) {
            onDataSetPath(data);
        }

        protected void onFieldPath(DataSet data, com.ibm.nmon.data.DataType type, String field) {
            onDataSetPath(data);
        }

        protected void onProcessPath(DataSet data) {
            onDataSetPath(data);
        }
    };
}
