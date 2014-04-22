package com.ibm.nmon.gui.table;

import java.util.BitSet;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;

/**
 * <p>
 * A base TableModel for tables whose column displayability is chosen at run time.
 * </p>
 * 
 * <p>
 * This class implements the standard TableModel methods by mapping the currently enabled column
 * indexes to the 'absolute' index values based on the total set of all possible columns. This
 * allows subclasses to maintain a constant mapping between column indexes and values. <em>All</em>
 * column indexes passed as parameters to abstract methods are <em>absolute</em> column indexes;
 * subclasses will never see relative (visible) column indexes.
 * </p>
 * 
 * <p>
 * For example, if a table has 3 possible columns and only columns 1 and 3 are enabled, subclasses
 * will still see requests for columns 1 and 3 in the <code>getEnabledXXX</code> methods. This
 * class, in the standard TableModel methods will map column 1 (relative) to column 1 (absolute) and
 * column 2 (visible) to column 3 (absolute). If columns 2 and 3 are enabled however, the mapping
 * will be column 1 to column 2 and column 2 to column 3. In both cases, the subclass sees the
 * absolute column indexes.
 * </p>
 */
public abstract class ChoosableColumnTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 9081831820888671224L;

    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    /**
     * The set of currently enabled columns. Subclasses are responsible for the life cycle of this
     * field. They <em>must</em> ensure that this set is the same size as the array returned by
     * {@link #getAllColumns()}.
     */
    // not final so that subclasses can add or remove columns as needed
    protected BitSet enabledColumns;

    /**
     * A map of column names to <em>absolute</em> column indexes. Subclasses <em>should not</em>
     * attempt to modify this map directly. Instead they should have {@link #getAllColumns()} return
     * an updated list then call {@link #buildColumnNameMap()} when the columns list changes.
     */
    protected Map<String, Integer> columnNamesMap;

    /**
     * @return an array of all possible column names
     */
    public abstract String[] getAllColumns();

    /**
     * @return <code>true</code> if the column is enabled by default
     */
    public abstract boolean getDefaultColumnState(int column);

    /**
     * @return <code>true</code> if the column can be disabled
     */
    public abstract boolean canDisableColumn(int column);

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    /**
     * @return the <em>enabled</em> column count
     */
    @Override
    public final int getColumnCount() {
        return enabledColumns.cardinality();
    }

    @Override
    public final Class<?> getColumnClass(int columnIndex) {
        int n = 0;

        for (int i = enabledColumns.nextSetBit(0); i >= 0; i = enabledColumns.nextSetBit(i + 1)) {
            if (n == columnIndex) {
                return getEnabledColumnClass(i);
            }

            ++n;
        }

        throw new ArrayIndexOutOfBoundsException(columnIndex);
    }

    protected abstract Class<?> getEnabledColumnClass(int columnIndex);

    @Override
    public final String getColumnName(int column) {
        int n = 0;

        for (int i = enabledColumns.nextSetBit(0); i >= 0; i = enabledColumns.nextSetBit(i + 1)) {
            if (n == column) {
                return getEnabledColumnName(i);
            }

            ++n;
        }

        throw new ArrayIndexOutOfBoundsException(column);
    }

    protected abstract String getEnabledColumnName(int column);

    @Override
    public final Object getValueAt(int row, int column) {
        int n = 0;

        for (int i = enabledColumns.nextSetBit(0); i >= 0; i = enabledColumns.nextSetBit(i + 1)) {
            if (n == column) {
                return getEnabledValueAt(row, i);
            }

            ++n;
        }

        throw new ArrayIndexOutOfBoundsException(column);
    }

    protected abstract Object getEnabledValueAt(int row, int column);

    /**
     * @return <code>true</code> if the named column is enabled; return <code>false</code> if the
     *         name is not valid for this table
     */
    public final boolean getEnabled(String columnName) {
        Integer idx = columnNamesMap.get(columnName);

        if (idx != null) {
            return enabledColumns.get(idx);
        }
        else {
            return false;
        }
    }

    /**
     * Set the state of the given column. This column index is <em>absolute</em>.
     */
    public final void setEnabled(String columnName, boolean enabled) {
        Integer idx = columnNamesMap.get(columnName);

        if ((idx != null) && (enabledColumns.get(idx) != enabled)) {
            logger.trace("{} enabled={}", columnName, enabled);

            enabledColumns.set(idx, enabled);

            if (enabled) {
                onEnable(columnName, idx);
            }
            else {
                onDisable(columnName, idx);
            }

            fireTableStructureChanged();
        }
    }

    /**
     * Hook function for subclasses to perform an action after a column is enabled. This method will
     * be called before a table model event is fired.
     */
    protected void onEnable(String columnName, int column) {}

    /**
     * Hook function for subclasses to perform an action after a column is disabled. This method
     * will be called before a table model event is fired.
     */
    protected void onDisable(String columnName, int column) {}

    /**
     * Rebuilds {@link #columnNamesMap} after calling {@link #getAllColumns()}.
     */
    protected void buildColumnNameMap() {
        Map<String, Integer> temp = new java.util.HashMap<String, Integer>();

        String[] columnNames = getAllColumns();

        for (int i = 0; i < columnNames.length; i++) {
            temp.put(columnNames[i], i);
        }

        columnNamesMap = java.util.Collections.unmodifiableMap(temp);
    }

    /**
     * @return the <em>absolute</em> index for the named column or <code>-1</code> if there is no
     *         column with the given name
     */
    public int getColumnIndex(String name) {
        Integer idx = columnNamesMap.get(name);

        if (idx == null) {
            return -1;
        }
        else {
            return idx;
        }
    }

    /**
     * @return the <em>relative</em> index for the named column or <code>-1</code> if there is no
     *         column with the given name
     */
    public int getEnabledColumnIndex(String name) {
        Integer idx = columnNamesMap.get(name);

        if (idx == null) {
            return -1;
        }
        else if (!enabledColumns.get(idx)) {
            return -1;
        }
        else {
            int n = 0;

            for (int i = 0; i < idx; i++) {
                if (enabledColumns.get(i)) {
                    ++n;
                }
            }

            return n;
        }
    }
}
