package com.ibm.nmon.gui.chart.summary;

import com.ibm.nmon.gui.table.ChoosableColumnTableModel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

import com.ibm.nmon.data.DataTuple;

import com.ibm.nmon.data.ProcessDataType;

import com.ibm.nmon.gui.chart.data.*;

import com.ibm.nmon.analysis.AnalysisRecord;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import static com.ibm.nmon.analysis.Statistic.*;

/**
 * Table model for {@link ChartSummaryPanel}. This model holds a single {@link DataTupleDataset}
 * which is used to display various summary data about the chart. For bar charts, this model
 * displays a row for each row / column combination in the chart DataSet. For line and interval line
 * charts, there will be one row for each series (i.e. line).
 */
public final class ChartSummaryTableModel extends ChoosableColumnTableModel {
    private static final long serialVersionUID = -4224937019632087892L;

    // check mark
    static final String VISIBLE = "\u2713";

    private static final String[] COLUMN_NAMES = new String[] { VISIBLE, "Hostname", "Data Type", "Metric",
            "Series Name", MINIMUM.toString(), AVERAGE.toString(), MAXIMUM.toString(), STD_DEV.toString(),
            MEDIAN.toString(), PERCENTILE_95.toString(), PERCENTILE_99.toString(), SUM.toString(), COUNT.toString(),
            "Graph " + MINIMUM.toString(), "Graph " + AVERAGE.toString(), "Graph " + MAXIMUM.toString(),
            "Graph " + STD_DEV.toString(), "Graph " + MEDIAN.toString(), "Graph " + PERCENTILE_95.toString(),
            "Graph " + PERCENTILE_99.toString(), "Graph " + SUM.toString(), "Graph " + COUNT.toString() };

    private final NMONVisualizerGui gui;

    private boolean[] defaultColumns;

    private DataTupleDataset dataset;

    private boolean[] rowVisible;

    // the GUI only creates a single summary table
    // so, cache which rows are selected for each tuple
    // note that the DataTuple itself cannot be used since it references a DataSet which could be a
    // memory leak as datasets are removed
    // so, use the hash code instead
    private final Map<Integer, boolean[]> rowVisibleCache;

    private PropertyChangeSupport propertyChangeSupport;

    public ChartSummaryTableModel(NMONVisualizerGui gui, String... defaultColumnNames) {
        this.gui = gui;
        this.propertyChangeSupport = new PropertyChangeSupport(this);

        buildColumnNameMap();

        // default default columns
        if (defaultColumnNames == null) {
            defaultColumnNames = new String[] { VISIBLE, "Data Type", "Metric", "Minimum", "Average", "Maximum",
                    "Std Dev" };
        }

        defaultColumns = new boolean[COLUMN_NAMES.length];
        java.util.Arrays.fill(defaultColumns, false);
        // enable/disable is always shown
        defaultColumns[0] = true;

        for (String columnName : defaultColumnNames) {
            int idx = getColumnIndex(columnName);

            if (idx != -1) {
                defaultColumns[idx] = true;
            }
            else {
                logger.warn("ignoring non-existent column '{}' for defaults", columnName);
            }
        }

        enabledColumns = new BitSet(COLUMN_NAMES.length);

        for (int i = 0; i < defaultColumns.length; i++) {
            enabledColumns.set(i, defaultColumns[i]);
        }

        dataset = null;
        rowVisibleCache = new java.util.HashMap<Integer, boolean[]>();
    }

    @Override
    public int getRowCount() {
        if (dataset == null) {
            return 0;
        }
        else if (dataset instanceof DataTupleCategoryDataset) {
            DataTupleCategoryDataset d = (DataTupleCategoryDataset) dataset;

            if (d.containsIntervals()) {
                return d.getRowCount();
            }
            else if (d.categoriesHaveDifferentStats()) {
                // different stats => no need to show a row per category
                return d.getColumnCount();
            }
            else {
                return d.getRowCount() * d.getColumnCount();
            }
        }
        else if (dataset instanceof DataTupleXYDataset) {
            return ((DataTupleXYDataset) dataset).getSeriesCount();
        }
        else if (dataset instanceof DataTupleHistogramDataset) {
            return ((DataTupleHistogramDataset) dataset).getSeriesCount();
        }
        else {
            return 0;
        }
    }

    @Override
    public String[] getAllColumns() {
        return COLUMN_NAMES;
    }

    @Override
    public boolean getDefaultColumnState(int column) {
        return defaultColumns[column];
    }

    @Override
    public boolean canDisableColumn(int column) {
        // all columns except visibility
        return column != 0;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 0 && enabledColumns.get(0);
    }

    @Override
    protected Class<?> getEnabledColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class;
        }
        else if (columnIndex < 5) {
            return String.class;
        }
        else if (columnIndex == 13) {
            // count
            return Integer.class;
        }
        else if (columnIndex == 22) {
            // graph count
            return Integer.class;
        }
        else {
            return Double.class;
        }
    }

    @Override
    protected String getEnabledColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    protected Object getEnabledValueAt(int row, int column) {
        DataTuple tuple = null;
        String seriesName = "";

        boolean graphDataOnly = false;

        if (dataset instanceof DataTupleCategoryDataset) {
            DataTupleCategoryDataset d = (DataTupleCategoryDataset) dataset;
            int columnCount = d.containsIntervals() ? d.getRowCount() : d.getColumnCount();

            int datasetRow = row / columnCount;
            int datasetColumn = row % columnCount;

            // System.out.println(row + " " + column + "\t" + datasetRow + " " + datasetColumn);

            tuple = d.getTuple(datasetRow, datasetColumn);

            // System.out.println(tuple.getDataSet().getHostname() + " " +
            // tuple.getDataType().toString() + " "
            // + tuple.getField() + "\t" + getColumnName(column));

            if (d.containsIntervals()) {
                seriesName = d.getRowKey(datasetColumn).toString();
            }
            else {
                if (d.categoriesHaveDifferentStats()) {
                    seriesName = d.getColumnKey(datasetColumn).toString();
                }
                else {
                    seriesName = d.getColumnKey(datasetColumn).toString() + " - " + d.getRowKey(datasetRow).toString();

                }
            }

            graphDataOnly = d.containsIntervals();
        }
        else if (dataset instanceof DataTupleXYDataset) {
            DataTupleXYDataset d = (DataTupleXYDataset) dataset;

            tuple = d.getTuple(row, -1);

            seriesName = d.getSeriesKey(row).toString();
        }
        else if (dataset instanceof DataTupleHistogramDataset) {
            DataTupleHistogramDataset d = (DataTupleHistogramDataset) dataset;

            tuple = d.getTuple(row, -1);

            seriesName = d.getSeriesKey(row).toString();
        }

        AnalysisRecord analysis = null;

        if (!graphDataOnly) {
            analysis = gui.getAnalysis(tuple.getDataSet());
        }

        switch (column) {
        case 0:
            return rowVisible[row];
        case 1:
            return tuple.getDataSet().getHostname();
        case 2:
            return tuple.getDataType().toString();
        case 3:
            return tuple.getField();
        case 4:
            return seriesName;
        case 5:
            return graphDataOnly ? dataset.getMinimum(row) : analysis.getMinimum(tuple.getDataType(), tuple.getField());
        case 6:
            return graphDataOnly ? dataset.getAverage(row) : analysis.getAverage(tuple.getDataType(), tuple.getField());
        case 7:
            return graphDataOnly ? dataset.getMaximum(row) : analysis.getMaximum(tuple.getDataType(), tuple.getField());
        case 8:
            return graphDataOnly ? dataset.getStandardDeviation(row) : analysis.getStandardDeviation(
                    tuple.getDataType(), tuple.getField());
        case 9:
            return graphDataOnly ? dataset.getMedian(row) : analysis.getMedian(tuple.getDataType(), tuple.getField());
        case 10:
            return graphDataOnly ? dataset.get95thPercentile(row) : analysis.get95thPercentile(tuple.getDataType(),
                    tuple.getField());
        case 11:
            return graphDataOnly ? dataset.get99thPercentile(row) : analysis.get99thPercentile(tuple.getDataType(),
                    tuple.getField());
        case 12:
            return graphDataOnly ? dataset.getSum(row) : analysis.getSum(tuple.getDataType(), tuple.getField());
        case 13:
            return graphDataOnly ? dataset.getCount(row) : analysis.getCount(tuple.getDataType(), tuple.getField());
        case 14:
            return dataset.getMinimum(row);
        case 15:
            return dataset.getAverage(row);
        case 16:
            return dataset.getMaximum(row);
        case 17:
            return dataset.getStandardDeviation(row);
        case 18:
            return dataset.getMedian(row);
        case 19:
            return dataset.get95thPercentile(row);
        case 20:
            return dataset.get99thPercentile(row);
        case 21:
            return dataset.getSum(row);
        case 22:
            return dataset.getCount(row);
        default:
            throw new ArrayIndexOutOfBoundsException(column);
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        // visibility is the only editable field
        boolean visible = (Boolean) value;

        if (rowVisible[rowIndex] != visible) {
            rowVisible[rowIndex] = visible;

            Object[] values = new Object[3];
            values[0] = getDatasetRow(rowIndex);
            values[1] = getDatasetColumn(columnIndex);
            values[2] = visible;

            propertyChangeSupport.firePropertyChange("rowVisible", null, values);
        }
    }

    // the table may represent a dataset that has both rows and columns of data
    // these functions convert table rows to dataset rows and columns
    //
    // row column table row
    // 0 0 0
    // 0 1 1
    // 1 0 2
    // 1 1 4
    //
    // for single column datasets, the row mapping is 1:1

    int getDatasetRow(int tableRow) {
        if (dataset instanceof DataTupleCategoryDataset) {
            DataTupleCategoryDataset d = (DataTupleCategoryDataset) dataset;

            if (d.containsIntervals()) {
                return tableRow;
            }
            else {
                int columnCount = d.getColumnCount();

                return tableRow / columnCount;
            }
        }
        else {
            return tableRow;
        }
    }

    int getDatasetColumn(int tableRow) {
        if (dataset instanceof DataTupleCategoryDataset) {
            DataTupleCategoryDataset d = (DataTupleCategoryDataset) dataset;

            if (d.containsIntervals()) {
                return -1;
            }
            else {
                int columnCount = d.getColumnCount();

                if (columnCount == 0) {
                    return -1;
                }
                else {
                    return tableRow % columnCount;
                }
            }
        }
        else {
            return -1;
        }
    }

    int getTableRow(int datasetRow, int datasetColumn) {
        if (dataset instanceof DataTupleCategoryDataset) {
            DataTupleCategoryDataset d = (DataTupleCategoryDataset) dataset;

            if (d.containsIntervals()) {
                return datasetRow;
            }
            else {
                int columnCount = d.getColumnCount();

                return datasetRow * columnCount + datasetColumn;
            }
        }
        else {
            return datasetRow;
        }
    }

    void setData(DataTupleDataset dataset) {
        if (this.dataset == dataset) {
            return;
        }

        this.dataset = dataset;

        if (dataset == null) {
            fireTableDataChanged();
            return;
        }

        int rowCount = getRowCount();

        // do not use DataTuple.hashCode() since we want the tuple's DataSet to be excluded
        // charts with the same types and fields should 'remember' the same row visibility
        int hashCode = 1;

        for (DataTuple t : dataset.getAllTuples()) {
            int typeHash = t.getDataType().hashCode();

            if (t.getDataType().getClass() == ProcessDataType.class) {
                // default process hash code uses process id
                com.ibm.nmon.data.Process process = ((ProcessDataType) t.getDataType()).getProcess();

                // all processes of the same name should display the same rows
                // but let the aggregated process be separate
                if (process.getId() != -1) {
                    typeHash = process.getName().hashCode();
                }
            }

            hashCode = (hashCode * 11) + (typeHash * 31) + (t.getField().hashCode() * 57);
        }

        rowVisible = rowVisibleCache.get(hashCode);

        if (rowVisible == null) {
            rowVisible = new boolean[rowCount];
            java.util.Arrays.fill(rowVisible, true);
            rowVisibleCache.put(hashCode, rowVisible);
        }

        if (dataset instanceof DataTupleCategoryDataset) {
            DataTupleCategoryDataset d = ((DataTupleCategoryDataset) dataset);

            if (!d.containsIntervals()) {
                // cannot change bar visibility
                setEnabled(VISIBLE, false);
            }
            else {
                setEnabled(VISIBLE, true);
            }
        }
        else if (dataset instanceof DataTupleXYDataset) {
            DataTupleXYDataset d = (DataTupleXYDataset) dataset;

            // cannot changed stacked charts
            if (d.isStacked()) {
                setEnabled(VISIBLE, false);
            }
            else {
                // cannot change visibility if only 1 line
                if (d.getSeriesCount() > 1) {
                    setEnabled(VISIBLE, true);
                }
                else {
                    setEnabled(VISIBLE, false);
                }
            }
        }
        else if (dataset instanceof DataTupleHistogramDataset) {
            DataTupleHistogramDataset d = (DataTupleHistogramDataset) dataset;

            // cannot change visibility if only 1 line
            if (d.getSeriesCount() > 1) {
                setEnabled(VISIBLE, true);
            }
            else {
                setEnabled(VISIBLE, false);
            }
        }

        if (logger.isTraceEnabled()) {
            if (dataset instanceof DataTupleXYDataset) {
                DataTupleXYDataset d = ((DataTupleXYDataset) dataset);
                List<String> series = new java.util.ArrayList<String>();

                for (int i = 0; i < d.getSeriesCount(); i++) {
                    series.add(d.getSeriesKey(i).toString());
                }

                logger.trace("set data for XY chart with {}", series);
            }
            else if (dataset instanceof DataTupleXYDataset) {
                DataTupleCategoryDataset d = (DataTupleCategoryDataset) dataset;
                Map<String, List<String>> series = new java.util.HashMap<String, List<String>>();

                for (int i = 0; i < d.getColumnCount(); i++) {
                    List<String> categories = new java.util.ArrayList<String>(3);
                    series.put(d.getColumnKey(i).toString(), categories);

                    for (int j = 0; j < d.getRowCount(); j++) {
                        categories.add(d.getRowKey(j).toString());
                    }
                }

                logger.trace("set data for category chart with {}", series);
            }
            else if (dataset instanceof DataTupleHistogramDataset) {
                DataTupleHistogramDataset d = ((DataTupleHistogramDataset) dataset);
                List<String> series = new java.util.ArrayList<String>();

                for (int i = 0; i < d.getSeriesCount(); i++) {
                    series.add(d.getSeriesKey(i).toString());
                }

                logger.trace("set data for histogram chart with {}", series);
            }
        }

        fireTableDataChanged();

        Object[] values = new Object[3];
        values[1] = getDatasetColumn(0);

        for (int i = 0; i < rowCount; i++) {
            values[0] = getDatasetRow(i);
            values[2] = rowVisible[i];
            propertyChangeSupport.firePropertyChange("rowVisible", null, values);
        }
    }

    void clear() {
        if (dataset != null) {
            // note that rowVisibleCache has already been added to visibleRowCache in setData()
            // no need to copy and store here

            dataset = null;
            fireTableDataChanged();
        }
    }

    void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }
}
