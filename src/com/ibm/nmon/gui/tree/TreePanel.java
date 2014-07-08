package com.ibm.nmon.gui.tree;

import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.DefaultTreeModel;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.ProcessDataSet;

import com.ibm.nmon.data.DataSetListener;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;
import com.ibm.nmon.data.ProcessDataType;

import com.ibm.nmon.data.Process;

import com.ibm.nmon.gui.dnd.TreeTransferHandler;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.util.DataHelper;

public final class TreePanel extends JScrollPane implements DataSetListener {
    private static final long serialVersionUID = 8763622839346467286L;

    static final String ROOT_NAME = "All Systems";

    protected final JTree tree;

    public void addTreeSelectionListener(TreeSelectionListener tsl) {
        tree.addTreeSelectionListener(tsl);
    }

    public TreePanel(NMONVisualizerGui gui) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(ROOT_NAME);

        tree = new JTree(root);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new TreeCellRenderer());

        setViewportView(tree);

        // make sure panel takes up entire parent, but the actual tree is offset slightly
        setBorder(null);
        tree.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        // data changes modify the tree
        gui.addDataSetListener(this);

        tree.addMouseListener(new TreeMouseListener(gui, tree));

        tree.setDragEnabled(true);
        tree.setTransferHandler(new TreeTransferHandler(gui));

        tree.setCellRenderer(new TreeCellRenderer());

        ToolTipManager.sharedInstance().registerComponent(tree);
    }

    @Override
    public final void dataAdded(DataSet data) {
        DefaultMutableTreeNode dataNode = null;
        TreePath selectedPath = null;
        List<TreePath> expandedPaths = null;
        boolean existing = false;
        int insertIdx = 0;

        // find the existing data set node, if any
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((DefaultTreeModel) tree.getModel()).getRoot();

        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode toSearch = (DefaultMutableTreeNode) root.getChildAt(i);
            DataSet currentData = (DataSet) toSearch.getUserObject();

            if (data.toString().compareTo(currentData.toString()) > 0) {
                insertIdx = i + 1;
            }

            if (currentData.equals(data)) {
                dataNode = toSearch;
                existing = true;

                // if the DataSet node has children, remove them since the DataTypes could have
                // changed; save the existing tree state first
                TreePath current = tree.getSelectionPath();

                if ((current != null) && new TreePath(dataNode.getPath()).isDescendant(current)) {
                    selectedPath = current;
                }

                java.util.Enumeration<TreePath> temp = tree.getExpandedDescendants(new TreePath(tree.getModel()
                        .getRoot()));

                if (temp != null) {
                    expandedPaths = java.util.Collections.list(temp);
                }
                else {
                    expandedPaths = java.util.Collections.emptyList();
                }

                dataNode.removeAllChildren();

                break;
            }
        }

        // new data set, new tree node
        if (dataNode == null) {
            dataNode = new DefaultMutableTreeNode(data);
        }

        buildDataSetTree(dataNode, data);

        // adding to an existing data set, alert the tree the structure has changed and make
        // sure the same nodes are expanded / selected
        if (existing) {
            ((javax.swing.tree.DefaultTreeModel) tree.getModel()).nodeStructureChanged(dataNode);

            for (TreePath path : expandedPaths) {
                tree.expandPath(rebuildPath(path));
            }

            if (selectedPath != null) {
                tree.setSelectionPath(rebuildPath(selectedPath));
            }
        }
        else {
            if (insertIdx > root.getChildCount()) {
                insertIdx = root.getChildCount() - 1;
            }

            ((DefaultTreeModel) tree.getModel()).insertNodeInto(dataNode, root, insertIdx);

            if (root.getChildCount() == 1) {
                // expand the root node now that there is something in the tree
                tree.expandRow(0);
            }

            selectedPath = tree.getSelectionPath();

            if ((selectedPath == null) || (selectedPath.getLastPathComponent() == root)) {
                // reselect the root node to ensure any TreeSelectionListeners (TreePathParsers)
                // are updated
                tree.setSelectionPath(null);
                tree.setSelectionPath(new TreePath(root));
            }
            // else leave the current selection alone
        }
    }

    @Override
    public final void dataRemoved(DataSet data) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((DefaultTreeModel) tree.getModel()).getRoot();

        for (int i = 0; i < root.getChildCount(); i++) {
            DataSet toSearch = (DataSet) ((DefaultMutableTreeNode) root.getChildAt(i)).getUserObject();

            if (toSearch.equals(data)) {
                Object removed = root.getChildAt(i);

                root.remove(i);

                ((javax.swing.tree.DefaultTreeModel) tree.getModel()).nodesWereRemoved(root, new int[] { i },
                        new Object[] { removed });

                // to remove a node requires it to be selected, so move the selection to the root
                tree.setSelectionPath(new TreePath(root));

                break;
            }
        }
    }

    @Override
    public void dataChanged(DataSet data) {
        dataAdded(data);
    }

    @Override
    public final void dataCleared() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((DefaultTreeModel) tree.getModel()).getRoot();

        // select the root node if any path is selected
        boolean pathSelected = false;

        TreePath path = tree.getSelectionPath();

        if (path != null) {
            pathSelected = true;
        }

        root.removeAllChildren();

        ((javax.swing.tree.DefaultTreeModel) tree.getModel()).reload();

        if (pathSelected) {
            tree.setSelectionPath(new TreePath(root));
        }
    }

    // since saved TreePaths may included nodes that no longer exist, rebuild the path with the
    // new, correct nodes
    @SuppressWarnings("unchecked")
    private TreePath rebuildPath(TreePath oldPath) {
        Object[] oldPaths = oldPath.getPath();
        DefaultMutableTreeNode[] newPath = new DefaultMutableTreeNode[oldPaths.length];

        DefaultMutableTreeNode parent = ((DefaultMutableTreeNode) tree.getModel().getRoot());

        newPath[0] = parent;
        int pathIdx = 1;

        while ((parent != null) && (pathIdx < newPath.length)) {
            List<DefaultMutableTreeNode> children = java.util.Collections
                    .list((java.util.Enumeration<DefaultMutableTreeNode>) parent.children());

            for (DefaultMutableTreeNode child : children) {
                if (((DefaultMutableTreeNode) oldPaths[pathIdx]).getUserObject().equals(child.getUserObject())) {
                    newPath[pathIdx] = child;
                    ++pathIdx;
                    parent = child;
                    break;
                }
            }
        }

        return new TreePath(newPath);
    }

    private void buildDataSetTree(DefaultMutableTreeNode dataNode, DataSet data) {
        boolean buildProcessNode = false;
        List<SubDataType> gcTypes = null;
        Map<String, DefaultMutableTreeNode> subtypes = null;

        // add types and processes
        for (DataType type : data.getTypes()) {
            if (type.getClass().equals(SubDataType.class)) {
                if (type.getId().startsWith("GC")) {
                    if (gcTypes == null) {
                        gcTypes = new java.util.ArrayList<SubDataType>();
                    }

                    gcTypes.add((SubDataType) type);
                }
                else {
                    if (subtypes == null) {
                        subtypes = new java.util.HashMap<String, DefaultMutableTreeNode>();
                    }

                    // create a single TreeNode for the subtype's id and cache it
                    SubDataType subtype = (SubDataType) type;
                    String id = subtype.getPrimaryId();

                    DefaultMutableTreeNode typeNode = subtypes.get(id);

                    if (typeNode == null) {
                        typeNode = new DefaultMutableTreeNode(id);

                        subtypes.put(id, typeNode);
                        dataNode.add(typeNode);
                    }

                    // add a sub-node for each type
                    typeNode.add(new TypeTreeNode(subtype, true));
                }
            }
            else if (type.getClass().equals(ProcessDataType.class)) {
                buildProcessNode = true;
            }
            else {
                dataNode.add(new TypeTreeNode(type));
            }
        }

        if (buildProcessNode) {
            dataNode.add(buildTopTree((ProcessDataSet) data));
        }

        if (gcTypes != null) {
            dataNode.add(buildGCTree(gcTypes));
        }
    }

    private DefaultMutableTreeNode buildTopTree(ProcessDataSet data) {
        // TOP
        // |_ process [single instance] (as ProcessDataType)
        // ...|_ TOP field 1
        // ...|_ TOP field 2
        // ...|_ TOP ...
        // ...|_ TOP field n
        // |_ process name [multiple instances] (String)
        // ...|_ aggregated process (as Process DataType)
        // ......|_ TOP fields
        // ...|_ process 1 (as Process DataType)
        // ......|_ TOP fields
        // ...|_ process 2
        // ......|_ TOP fields
        // ...|_ ...
        // ...|_ process n
        // ......|_ TOP fields

        // format topNode's name like TypeTreeNode
        DefaultMutableTreeNode topNode = new DefaultMutableTreeNode(data.getTypeIdPrefix());

        Map<String, List<Process>> processNameToProcesses = DataHelper.getProcessesByName(data, true);

        for (String processName : processNameToProcesses.keySet()) {
            List<Process> processes = processNameToProcesses.get(processName);

            DefaultMutableTreeNode processNode = null;

            // more than 1 process with the given name => add a subtree
            if (processes.size() > 1) {
                // process node will be an AggregateDataType; sub-tree contains processs
                processNode = new DefaultMutableTreeNode(processes.get(0).getName());

                List<DataType> types = new java.util.ArrayList<DataType>();

                for (Process process : processes) {
                    DefaultMutableTreeNode processInstance = new DefaultMutableTreeNode(process);

                    DataType type = data.getType(process);

                    types.add(type);

                    for (String field : type.getFields()) {
                        processInstance.add(new DefaultMutableTreeNode(field));
                    }

                    processNode.add(processInstance);
                }
            }
            else {
                // process node is the process; no sub-tree
                processNode = new DefaultMutableTreeNode(processes.get(0));

                for (String field : data.getType(processes.get(0)).getFields()) {
                    processNode.add(new DefaultMutableTreeNode(field));
                }
            }

            topNode.add(processNode);
        }

        return topNode;
    }

    private DefaultMutableTreeNode buildGCTree(List<SubDataType> gcTypes) {
        // create a top-level GC node
        DefaultMutableTreeNode gcNode = new DefaultMutableTreeNode("GC");
        Map<String, DefaultMutableTreeNode> jvmNodes = new java.util.HashMap<String, DefaultMutableTreeNode>();

        // under GC, create a node for each JVM to contain all the data types for that JVM
        for (SubDataType type : gcTypes) {
            String jvmName = ((SubDataType) type).getSubId();

            DefaultMutableTreeNode jvmNode = jvmNodes.get(jvmName);

            if (jvmNode == null) {
                jvmNode = new DefaultMutableTreeNode(jvmName);

                gcNode.add(jvmNode);
                jvmNodes.put(jvmName, jvmNode);
            }

            jvmNode.add(new TypeTreeNode(type, false));
        }

        return gcNode;
    }

    private final class TypeTreeNode extends DefaultMutableTreeNode {
        private static final long serialVersionUID = -3704741016840510282L;

        private final String toDisplay;

        TypeTreeNode(DataType type) {
            super(type);

            toDisplay = type.getId();

            for (String field : type.getFields()) {
                add(new DefaultMutableTreeNode(field));
            }
        }

        TypeTreeNode(SubDataType type, boolean showSubId) {
            super(type);

            if (showSubId) {
                toDisplay = type.getSubId();
            }
            else {
                toDisplay = type.getPrimaryId();
            }

            for (String field : type.getFields()) {
                add(new DefaultMutableTreeNode(field));
            }
        }

        @Override
        public String toString() {
            return toDisplay;
        }
    }
}
