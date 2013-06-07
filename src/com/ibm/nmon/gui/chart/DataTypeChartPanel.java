package com.ibm.nmon.gui.chart;

import java.beans.PropertyChangeEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.JFreeChart;

import com.ibm.nmon.gui.chart.builder.LineChartBuilder;
import com.ibm.nmon.gui.chart.builder.LineChartBuilderPlugin;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.definition.ExactDataDefinition;
import com.ibm.nmon.data.definition.NamingMode;

import com.ibm.nmon.interval.IntervalListener;
import com.ibm.nmon.interval.Interval;

/**
 * Display a time series plot of the selected data.
 */
public final class DataTypeChartPanel extends LineChartPanel implements IntervalListener {
    private final LineChartBuilder chartBuilder;

    private ExactDataDefinition definition;

    public DataTypeChartPanel(NMONVisualizerGui gui) {
        super(gui);

        chartBuilder = new LineChartBuilder();
        chartBuilder.addPlugin(new LineChartBuilderPlugin(gui));
    }

    public void setData(DataSet data, DataType type) {
        setData(data, type, type.getFields());
    }

    public void setData(DataSet data, DataType type, List<String> fields) {
        definition = new ExactDataDefinition(data, type, fields);

        displayChart();
    }

    public DataSet getData() {
        return definition.getDataSet();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);

            if (enabled) {
                gui.addPropertyChangeListener("granularity", this);
                gui.getIntervalManager().addListener(this);

                chartBuilder.setInterval(gui.getIntervalManager().getCurrentInterval());
                chartBuilder.setGranularity(gui.getGranularity());

                if (definition != null) {
                    displayChart();
                }
            }
            else {
                gui.removePropertyChangeListener("granularity", this);
                gui.getIntervalManager().removeListener(this);

                // _super_ => clear chart but not the data
                super.clearChart();
            }
        }
    }

    @Override
    public void clearChart() {
        super.clearChart();

        this.definition = null;
    }

    @Override
    protected String getSaveFileName() {
        DataType type = definition.getDataType();
        List<String> fields = definition.getMatchingFields(type);

        return definition.getDataSet().getHostname() + '_' + type.toString()
                + (fields.size() == 1 ? '_' + fields.get(0) : "");
    }

    @Override
    public void intervalAdded(Interval interval) {}

    @Override
    public void intervalRemoved(Interval interval) {}

    @Override
    public void intervalsCleared() {}

    @Override
    public void currentIntervalChanged(Interval interval) {
        chartBuilder.setInterval(interval);

        if (getChart() != null) {
            displayChart();
        }
    }

    @Override
    public void intervalRenamed(Interval interval) {}

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);

        if ("granularity".equals(evt.getPropertyName())) {
            chartBuilder.setGranularity((Integer) evt.getNewValue());

            if (getChart() != null) {
                displayChart();
            }
        }
    }

    private void displayChart() {
        if (isEnabled()) {
            long startT = System.nanoTime();

            DataSet data = definition.getDataSet();
            DataType type = definition.getDataType();
            List<String> fields = definition.getMatchingFields(type);

            chartBuilder.initChart();
            chartBuilder.addData(definition, data, NamingMode.FIELD);

            JFreeChart chart = chartBuilder.getChart();

            String fieldLabel = "";
            String axisLabel = "";

            if (fields.size() == 1) {
                fieldLabel = " - " + fields.get(0);
                axisLabel = getAxisLabel(type, fields.get(0));
            }
            else {
                axisLabel = getAxisLabel(type);
            }

            chart.setTitle(type + fieldLabel + '\n' + data.getHostname());
            chart.getXYPlot().getRangeAxis().setLabel(axisLabel);

            boolean percent = true;

            if (!type.getName().contains("%")) {
                for (String field : fields) {
                    if (!field.contains("%")) {
                        percent = false;
                        break;
                    }
                    if (PERCENT_AXIS_EXCEPTIONS.contains(field)) {
                        percent = false;
                        break;
                    }
                }
            }

            if (percent) {
                LineChartBuilder.setPercentYAxis(chart);
            }

            if (logger.isTraceEnabled()) {
                logger.trace("{}: {}-{} chart created in {}ms",
                        new Object[] { data.getHostname(), type.getId(),
                                fields.size() == type.getFieldCount() ? '*' : fields,
                                (System.nanoTime() - startT) / 1000000.0d });
            }

            setChart(chart);
        }
    }

    private String getAxisLabel(DataType type) {
        if ("CPU_ALL".equals(type.getId())) {
            return "";
        }
        else if (type.getId().startsWith("CPU")) {
            return "% CPU";
        }
        else {
            String label = TYPE_AXIS_NAMES.get(type.getId());

            if (label == null) {
                int idx = type.getId().indexOf('(');

                if (idx != -1) {
                    label = TYPE_AXIS_NAMES.get(type.getId().substring(0, idx - 1));
                }
            }

            if (label == null) {
                return "";
            }
            else {
                return label;
            }
        }
    }

    private String getAxisLabel(DataType type, String field) {
        String label = FIELD_AXIS_NAMES.get(field);

        if (label != null) {
            return label;
        }
        else if (field.endsWith("KB/s")) {
            return "KB / s";
        }
        else if (field.contains("packets")) {
            return "Packets / s";
        }
        else if (field.startsWith("nr_")) {
            return "Count";
        }
        else if (field.startsWith("pg")) {
            return "Pages / s";
        }
        else if (field.endsWith("ch")) {
            return "Characters / s";
        }
        else if (field.endsWith("_freed")) {
            return "MB";
        }
        else if (field.contains("bytes")) {
            return "Bytes";
        }
        else if (field.contains("count")) {
            return "Count";
        }
        else if (field.contains("MB")) {
            return "MB";
        }
        else {
            return getAxisLabel(type);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        gui.removePropertyChangeListener("granularity", this);
    }

    private static final Map<String, String> TYPE_AXIS_NAMES;
    private static final Map<String, String> FIELD_AXIS_NAMES;
    private static final Set<String> PERCENT_AXIS_EXCEPTIONS;

    static {
        Map<String, String> tempTypes = new java.util.HashMap<String, String>();
        Map<String, String> tempFields = new java.util.HashMap<String, String>();
        Set<String> tempExceptions = new java.util.HashSet<String>();

        tempTypes.put("DISKBSIZE", "KB / Block");
        tempTypes.put("DISKBUSY", "% Busy");
        tempTypes.put("DISKREAD", "KB / s");
        tempTypes.put("DISKWRITE", "KB / s");
        tempTypes.put("DISKRXFER", "IO Ops / s");
        tempTypes.put("DISKXFER", "IO Ops / s");
        tempTypes.put("JFSFILE", "% Used");
        tempTypes.put("JFSINODE", "% Used");
        tempTypes.put("MEMNEW", "% Used");
        tempTypes.put("NET", "KB / s");
        tempTypes.put("NETPACKET", "Packets / s");
        tempTypes.put("NETERROR", "Errors / s");
        tempTypes.put("NETSIZE", "Bytes / Packet");
        tempTypes.put("GCAFT", "MB");
        tempTypes.put("GCBEF", "MB");
        tempTypes.put("GCSINCE", "Seconds");
        tempTypes.put("GCTIME", "Milliseconds");
        tempTypes.put("GCCOUNT", "Count");
        tempTypes.put("SEA", "KB / s");
        tempTypes.put("SEAPACKET", "Packets / s");
        tempTypes.put("FCREAD", "KB / s");
        tempTypes.put("FCWRITE", "KB / s");
        tempTypes.put("FCXFERIN", "Frames / s");
        tempTypes.put("FCXFEROUT", "Frames / s");
        tempTypes.put("RESP", "Seconds");

        tempFields.put("CPUs", "Count");
        tempFields.put("CPU%", "% CPU");
        tempFields.put("User%", "% CPU");
        tempFields.put("Sys%", "% CPU");
        tempFields.put("Wait%", "% CPU");

        tempFields.put("%CPU", "% CPU");
        tempFields.put("%Usr", "% CPU");
        tempFields.put("%Sys", "% CPU");
        tempFields.put("%Wait", "% CPU");

        tempFields.put("Runnable", "Count");
        tempFields.put("pswitch", "Switches / s");
        tempFields.put("syscall", "Calls / s");
        tempFields.put("fork", "Forks / s");
        tempFields.put("forks", "Forks / s");

        tempFields.put("pgfault", "Faults / s");
        tempFields.put("pgmajfault", "Faults / s");
        tempFields.put("faults", "Faults / s");
        tempFields.put("pgpgin", "KB / s");
        tempFields.put("pgpout", "KB / s");
        tempFields.put("pswpin", "Pages / s");
        tempFields.put("pswpout", "Pages / s");
        tempFields.put("Paging", "Pages / s");

        tempFields.put("Size", "MB");
        tempFields.put("ResSet", "MB");
        tempFields.put("ResText", "MB");
        tempFields.put("ResData", "MB");
        tempFields.put("ShdLib", "MB");

        tempFields.put("MajorFault", "Pages / s");
        tempFields.put("MinorFault", "Pages / s");

        tempFields.put("cycles", "Count / s");

        tempFields.put("finalizers", "Count");
        tempFields.put("soft", "Count");
        tempFields.put("weak", "Count");
        tempFields.put("phantom", "Count");
        tempFields.put("tiltratio", "%");

        tempFields.put("requested", "bytes");

        tempFields.put("flipped", "Objects");
        tempFields.put("tenured", "Objects");
        tempFields.put("moved", "Objects");
        
        tempFields.put("throughput", "Tx / s");
        tempFields.put("hits", "Hits / s");

        tempExceptions.add("EC_User%");
        tempExceptions.add("EC_Sys%");
        tempExceptions.add("EC_Wait%");
        tempExceptions.add("EC_CPU%");
        tempExceptions.add("VP_User%");
        tempExceptions.add("VP_Sys%");
        tempExceptions.add("VP_Wait%");
        tempExceptions.add("VP_CPU%");

        TYPE_AXIS_NAMES = java.util.Collections.unmodifiableMap(tempTypes);
        FIELD_AXIS_NAMES = java.util.Collections.unmodifiableMap(tempFields);
        PERCENT_AXIS_EXCEPTIONS = java.util.Collections.unmodifiableSet(tempExceptions);
    }
}
