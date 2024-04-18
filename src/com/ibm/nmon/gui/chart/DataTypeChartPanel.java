package com.ibm.nmon.gui.chart;

import java.beans.PropertyChangeEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.JFreeChart;

import com.ibm.nmon.gui.chart.builder.ChartFormatter;
import com.ibm.nmon.gui.chart.builder.LineChartBuilder;
import com.ibm.nmon.gui.chart.builder.LineChartBuilderPlugin;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;
import com.ibm.nmon.data.ProcessDataType;

import com.ibm.nmon.data.definition.ExactDataDefinition;
import com.ibm.nmon.data.definition.NamingMode;

import com.ibm.nmon.chart.definition.LineChartDefinition;

import com.ibm.nmon.interval.IntervalListener;
import com.ibm.nmon.interval.Interval;

/**
 * Display a time series plot of the selected data.
 */
public final class DataTypeChartPanel extends LineChartPanel implements IntervalListener {
    private static final long serialVersionUID = 7780208253045360843L;

    private final LineChartBuilder chartBuilder;

    private ExactDataDefinition definition;

    public DataTypeChartPanel(NMONVisualizerGui gui) {
        super(gui, gui.getMainFrame());

        chartBuilder = new LineChartBuilder();
        chartBuilder.addPlugin(new LineChartBuilderPlugin(gui));
        chartBuilder.setFormatter(gui.getChartFormatter());
    }

    public void setData(DataSet data, DataType type) {
        setData(data, type, type.getFields());
    }

    public void setData(DataSet data, DataType type, List<String> fields) {
        definition = new ExactDataDefinition(data, type, fields);

        displayChart();
    }

    public DataSet getData() {
        return definition == null ? null : definition.getDataSet();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            super.setEnabled(enabled);

            if (enabled) {
                gui.addPropertyChangeListener("granularity", this);
                gui.addPropertyChangeListener("chartFormatter", this);
                gui.addPropertyChangeListener("lineChartLegend", this);
                gui.getIntervalManager().addListener(this);

                chartBuilder.setInterval(gui.getIntervalManager().getCurrentInterval());
                chartBuilder.setGranularity(gui.getGranularity());
                chartBuilder.setFormatter(gui.getChartFormatter());
                chartBuilder.showLegends(gui.getBooleanProperty("lineChartLegend"));

                if (definition != null) {
                    displayChart();
                }
            }
            else {
                gui.removePropertyChangeListener("granularity", this);
                gui.removePropertyChangeListener("chartFormatter", this);
                gui.removePropertyChangeListener("lineChartLegend", this);
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
    protected String validateSaveFileName(String filename) {
        DataType type = definition.getDataType();
        List<String> fields = definition.getMatchingFields(type);

        return super.validateSaveFileName(definition.getDataSet().getHostname() + '_' + type.toString()
                + (fields.size() == 1 ? '_' + fields.get(0) : ""));
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
        else if ("chartFormatter".equals(evt.getPropertyName())) {
            chartBuilder.setFormatter((ChartFormatter) evt.getNewValue());
            displayChart();
        }
        else if ("lineChartLegend".equals(evt.getPropertyName())) {
            boolean showLegend = (Boolean) evt.getNewValue();

            chartBuilder.showLegends(showLegend);

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

            String fieldLabel = "";
            String axisLabel = "";

            if (fields.size() == 1) {
                fieldLabel = " - " + fields.get(0);
                axisLabel = getAxisLabel(type, fields.get(0));
            }
            else {
                axisLabel = getAxisLabel(type);
            }

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

                    if ((type.getClass() == ProcessDataType.class) && gui.getBooleanProperty("scaleProcessesByCPUs")) {
                        percent = false;
                        break;
                    }
                }
            }

            LineChartDefinition chartDefinition = new LineChartDefinition("", type + fieldLabel);
            chartDefinition.setYAxisLabel(axisLabel);
            chartDefinition.setUsePercentYAxis(percent);

            setSaveSize(chartDefinition.getWidth(), chartDefinition.getHeight());

            chartBuilder.initChart(chartDefinition);
            chartBuilder.addLinesForData(definition, data, NamingMode.FIELD);

            JFreeChart chart = chartBuilder.getChart();

            if (logger.isDebugEnabled()) {
                logger.debug("{}: {}-{} chart created in {}ms",
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
        else if (field.endsWith("kb/s")) {
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
        else if (field.toLowerCase().contains("megabytes")) {
            return "MB";
        }
        else if (field.toLowerCase().contains("mbytes")) {
            return "MB";
        }
        else if (field.toLowerCase().contains("kbytes")) {
            return "KB";
        }
        else if (field.toLowerCase().contains("bytes")) {
            return "Bytes";
        }
        else if (field.contains("count")) {
            return "Count";
        }
        else if (field.endsWith("kb")) {
            return "KB";
        }
        else if (field.endsWith("mb")) {
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
        gui.getIntervalManager().removeListener(this);
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
        tempTypes.put("IOStat CPU", "%");
        tempTypes.put(SubDataType.buildId("IOStat Device", "%util"), "% Utilization");
        tempTypes.put(SubDataType.buildId("IOStat Device", "rMB/s"), "MB / s");
        tempTypes.put(SubDataType.buildId("IOStat Device", "wMB/s"), "MB / s");
        tempTypes.put(SubDataType.buildId("IOStat Device", "await"), "ms");
        tempTypes.put(SubDataType.buildId("IOStat Device", "r_await"), "ms");
        tempTypes.put(SubDataType.buildId("IOStat Device", "w_await"), "ms");
        tempTypes.put(SubDataType.buildId("IOStat Device", "svctm"), "ms");
        tempTypes.put(SubDataType.buildId("IOStat ZPool", "alloc"), "GB");
        tempTypes.put(SubDataType.buildId("IOStat ZPool", "free"), "GB");
        tempTypes.put(SubDataType.buildId("IOStat ZPool", "rMB/s"), "MB / s");
        tempTypes.put(SubDataType.buildId("IOStat ZPool", "wMB/s"), "MB / s");
        tempTypes.put("LAT", "μs");
        tempTypes.put("CLAT", "μs");
        tempTypes.put("SLAT", "μs");
        tempTypes.put("BW", "KB / s");

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

        tempFields.put("Size", "KB");
        tempFields.put("ResSet", "KB");
        tempFields.put("ResText", "KB");
        tempFields.put("ResData", "KB");
        tempFields.put("ShdLib", "KB");

        tempFields.put("MajorFault", "Pages / s");
        tempFields.put("MinorFault", "Pages / s");

        tempFields.put("cycles", "Count / s");

        tempFields.put("finalizers", "Count");
        tempFields.put("soft", "Count");
        tempFields.put("weak", "Count");
        tempFields.put("phantom", "Count");
        tempFields.put("tiltratio", "%");

        tempFields.put("requested", "Bytes");

        tempFields.put("flipped", "Objects");
        tempFields.put("tenured", "Objects");
        tempFields.put("moved", "Objects");

        tempFields.put("throughput", "Tx / s");
        tempFields.put("hits", "Hits / s");

        tempExceptions.add("EC_Used%");
        tempExceptions.add("VP_User%");
        tempExceptions.add("VP_Sys%");
        tempExceptions.add("VP_Wait%");
        tempExceptions.add("VP_CPU%");

        TYPE_AXIS_NAMES = java.util.Collections.unmodifiableMap(tempTypes);
        FIELD_AXIS_NAMES = java.util.Collections.unmodifiableMap(tempFields);
        PERCENT_AXIS_EXCEPTIONS = java.util.Collections.unmodifiableSet(tempExceptions);
    }
}
