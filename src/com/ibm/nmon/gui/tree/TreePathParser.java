package com.ibm.nmon.gui.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.SystemDataSet;

import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.Process;

/**
 * Parse a TreePath based on the JTree built by {@link TreePanel}. Determines the given DataSet,
 * DataType and field, any of which can be null.
 */
public abstract class TreePathParser {
    public final Object parse(TreePath path) {
        if (path == null) {
            onNullPath();
            return onReturn(null, null, null);
        }

        Object[] paths = path.getPath();

        if (paths.length == 0) {
            onNullPath();
            return onReturn(null, null, null);
        }

        if (paths.length == 1) {
            onRootPath();
            return onReturn(null, null, null);
        }

        SystemDataSet data = (SystemDataSet) ((DefaultMutableTreeNode) paths[1]).getUserObject();

        // data set
        if (paths.length == 2) {
            onDataSetPath(data);
            return onReturn(data, null, null);
        }

        DataType type = null;
        String field = null;

        if (paths.length > 2) {
            Object o = ((DefaultMutableTreeNode) paths[2]).getUserObject();

            if (!o.getClass().equals(String.class)) {
                type = (DataType) o;
            }
            // String => TOP, GC or SubDataType
        }

        if (paths.length > 3) {
            Object o = ((DefaultMutableTreeNode) paths[3]).getUserObject();

            if (o.getClass().equals(Process.class)) {
                type = data.getType((Process) o);
            }
            else if (o.getClass().equals(String.class)){
                field = (String) o;
            }
            else {
                type = (DataType) o;    // SubDataType
            }
        }

        if (paths.length > 4) {
            field = null; // previous string may be a process name

            Object o = ((DefaultMutableTreeNode) paths[4]).getUserObject();

            if (o.getClass().equals(Process.class)) {
                type = data.getType((Process) o);
            }
            else if (o instanceof DataType) {
                type = (DataType) o;
            }
            else {
                field = (String) o;
            }
        }

        if (paths.length > 5) {
            Object o = ((DefaultMutableTreeNode) paths[5]).getUserObject();

            field = (String) o;
        }

        if (type == null) {
            onProcessPath(data);
        }
        else {
            if (field == null) {
                onTypePath(data, type);
            }
            else {
                onFieldPath(data, type, field);
            }
        }

        return onReturn(data, type, field);
    }

    /**
     * Called when the TreePath is actually not pointing to any node. After calling this,
     * <code>parse</code> calls <code>onReturn</code> with all null arguments.
     */
    protected void onNullPath() {}

    /**
     * Called when the TreePath is pointing to the root node. After calling this, <code>parse</code>
     * <code>parse</code> calls <code>onReturn</code> with all null arguments.
     */
    protected void onRootPath() {}

    /**
     * Called when the TreePath is pointing to an DataSet.
     */
    protected void onDataSetPath(DataSet data) {}

    /**
     * Called when the TreePath is pointing to a DataType. Note this includes Process instances
     * since each Process also defines its own DataType.
     */
    protected void onTypePath(DataSet data, DataType type) {}

    /**
     * Called when the TreePath is pointing to a process name, but not a specific Process instance.
     */
    protected void onProcessPath(DataSet data) {}

    /**
     * Called when the TreePath is pointing to a specific field from a DataType.
     */
    protected void onFieldPath(DataSet data, DataType type, String field) {}

    /**
     * Called after a TreePath is successfully parsed and the path is on a tree node. Subclasses may
     * return <code>null</code>. The arguments can be <code>null</code> depending on what type of
     * node the path points to.
     */
    protected Object onReturn(DataSet data, DataType type, String field) {
        return null;
    }
}
