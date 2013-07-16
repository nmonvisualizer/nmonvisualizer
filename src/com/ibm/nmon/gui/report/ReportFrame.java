package com.ibm.nmon.gui.report;

import org.slf4j.Logger;

import javax.swing.JFrame;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListCellRenderer;

import javax.swing.JSplitPane;
import javax.swing.JOptionPane;

import javax.swing.JFileChooser;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.util.List;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataSetListener;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.gui.file.GUIFileChooser;

import com.ibm.nmon.gui.Styles;

public final class ReportFrame extends JFrame implements DataSetListener {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ReportFrame.class);

    private final NMONVisualizerGui gui;

    private final JPanel blank;
    private ReportPanel reportPanel;

    private final JList<DataSet> systems;

    public ReportFrame(NMONVisualizerGui gui) {
        super("Custom Report");

        this.gui = gui;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);
        setIconImage(Styles.REPORT_ICON.getImage());

        // Centered in the main frame at 90% of its size
        Dimension parentSize = gui.getMainFrame().getSize();
        Dimension thisSize = new Dimension((int) (parentSize.getWidth() * 0.9), (int) (parentSize.getHeight() * 0.9));
        setSize(thisSize);

        // (1 - .9) / 2 = .05
        Point parentLocation = gui.getMainFrame().getLocation();
        Point thisLocation = new Point((int) (parentLocation.getX() + (parentSize.getWidth() * 0.05)),
                (int) (parentLocation.getY() + (parentSize.getHeight() * 0.05)));
        setLocation(thisLocation);

        // maximize if main fram is also maximized
        // size and location will still be set and remembered as above
        if ((gui.getMainFrame().getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }

        setJMenuBar(new ReportMenu(this));

        // tree of parsed files on the left, content on the left
        JSplitPane lrSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        // never resize list automatically
        lrSplitPane.setResizeWeight(0);
        lrSplitPane.setBorder(null);

        blank = new JPanel();
        blank.setBackground(java.awt.Color.WHITE);
        blank.setBorder(Styles.createTopLineBorder(blank));

        ReportSystemsListModel model = new ReportSystemsListModel();

        systems = new JList<DataSet>(model);
        systems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        systems.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // see ReportSystemsListModel, which manages the indexing
                if (index == 0) {
                    setText("All Systems");
                    setIcon(Styles.REPORT_ICON);
                }
                else {
                    setText(((DataSet) value).getHostname());
                    setIcon(Styles.COMPUTER_ICON);
                }

                setBorder(new EmptyBorder(2, 5, 2, 5));

                return this;
            }
        });

        systems.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    DataSet selected = systems.getModel().getElementAt(
                            systems.getSelectionModel().getMinSelectionIndex());

                    if (selected == null) {
                        if (reportPanel != null) {
                            reportPanel.setData(ReportFrame.this.gui.getDataSets());
                        }

                        enableMultiplexing(false);
                    }
                    else {
                        if (reportPanel != null) {
                            reportPanel.setData(java.util.Collections.singletonList(selected));
                        }

                        // systems.setSelectedIndex(0);
                        enableMultiplexing(true);
                    }
                }
            }
        });

        for (DataSet data : gui.getDataSets()) {
            model.addData(data);
        }

        JRadioButton none = new JRadioButton("None");
        JRadioButton byType = new JRadioButton("By Type");
        JRadioButton byField = new JRadioButton("By Field");

        none.setActionCommand(ReportPanel.MultiplexMode.NONE.name());
        byType.setActionCommand(ReportPanel.MultiplexMode.BY_TYPE.name());
        byField.setActionCommand(ReportPanel.MultiplexMode.BY_FIELD.name());

        none.setFont(Styles.LABEL);
        byType.setFont(Styles.LABEL);
        byField.setFont(Styles.LABEL);

        none.setBorder(Styles.CONTENT_BORDER);
        byType.setBorder(Styles.CONTENT_BORDER);
        byField.setBorder(Styles.CONTENT_BORDER);

        ActionListener modeChanger = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (reportPanel != null) {
                    reportPanel.setMultiplexMode(ReportPanel.MultiplexMode.valueOf(e.getActionCommand()));
                }
            }
        };

        none.addActionListener(modeChanger);
        byType.addActionListener(modeChanger);
        byField.addActionListener(modeChanger);

        none.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(none);
        group.add(byType);
        group.add(byField);

        JLabel multiplex = new JLabel("Chart Multiplexing:");
        multiplex.setFont(Styles.LABEL);

        JPanel top = new JPanel();
        top.add(multiplex);
        top.add(none);
        top.add(byType);
        top.add(byField);

        JPanel right = new JPanel(new BorderLayout());

        right.add(top, BorderLayout.PAGE_START);
        right.add(blank, BorderLayout.CENTER);

        lrSplitPane.setLeftComponent(systems);
        lrSplitPane.setRightComponent(right);

        setContentPane(lrSplitPane);

        systems.setSelectedIndex(0);
        enableMultiplexing(false);

        gui.addDataSetListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // can only update the divider location when the window is visible
                // charts get 80%
                ((JSplitPane) getContentPane()).setDividerLocation(.2);
            }
        });
    }

    @Override
    public void dataAdded(DataSet data) {
        ((ReportSystemsListModel) systems.getModel()).addData(data);

        if (reportPanel != null) {
            // if All Systems selected add the data; otherwise do not update the chart
            if (systems.getSelectedIndex() == 0) {
                reportPanel.addData(data);
            }
        }
    }

    @Override
    public void dataRemoved(DataSet data) {
        ((ReportSystemsListModel) systems.getModel()).removeData(data);

        if (reportPanel != null) {
            reportPanel.removeData(data);
        }
    }

    @Override
    public void dataChanged(DataSet data) {
        ((ReportSystemsListModel) systems.getModel()).dataChanged();

        if (reportPanel != null) {
            reportPanel.resetReport();
        }
    }

    @Override
    public void dataCleared() {
        ((ReportSystemsListModel) systems.getModel()).clearData();

        if (reportPanel != null) {
            reportPanel.clearData();
        }
    }

    private void enableMultiplexing(boolean enable) {
        JSplitPane lrSplitPane = (JSplitPane) getContentPane();
        JPanel top = (JPanel) ((JPanel) lrSplitPane.getRightComponent()).getComponent(0);

        for (int i = 1; i < 4; i++) {
            top.getComponent(i).setEnabled(enable);
        }
    }

    private ReportPanel.MultiplexMode getMultiplexMode() {
        JSplitPane lrSplitPane = (JSplitPane) getContentPane();
        JPanel top = (JPanel) ((JPanel) lrSplitPane.getRightComponent()).getComponent(0);

        for (int i = 1; i < 4; i++) {
            JRadioButton button = (JRadioButton) top.getComponent(i);
            if (button.isSelected()) {
                return ReportPanel.MultiplexMode.valueOf(button.getActionCommand());
            }
        }

        // should not happen
        return null;
    }

    @Override
    public void dispose() {
        if (reportPanel != null) {
            reportPanel.dispose();
            reportPanel = null;
        }

        gui.removeDataSetListener(this);

        super.dispose();
    }

    NMONVisualizerGui getGui() {
        return gui;
    }

    void loadReportDefinition(File reportFile) {
        if (reportPanel != null) {
            reportPanel.dispose();
        }

        if (!reportFile.exists()) {
            int result = JOptionPane.showConfirmDialog(this, "File '" + reportFile.getName() + "' is not a valid file",
                    "Invalid File", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                return;
            }
        }

        JSplitPane lrSplitPane = (JSplitPane) getContentPane();
        ((JPanel) lrSplitPane.getRightComponent()).remove(1);

        try {
            gui.getReportCache().addReport("custom", reportFile.getAbsolutePath());

            List<DataSet> datasets = new java.util.ArrayList<DataSet>(gui.getDataSetCount());

            for (DataSet data : ReportFrame.this.gui.getDataSets()) {
                datasets.add(data);
            }

            reportPanel = new ReportPanel(ReportFrame.this.gui, ReportFrame.this, "custom", datasets,
                    getMultiplexMode());
            ((JPanel) lrSplitPane.getRightComponent()).add(reportPanel);

            reportPanel.setEnabled(true);
            validate();

            setTitle("Custom Report" + " - " + reportFile.getName());
        }
        catch (Exception e) {
            ((JPanel) lrSplitPane.getRightComponent()).add(blank);
            validate();
            reportPanel = null;

            setTitle("Custom Report");

            LOGGER.error("could not parse report file '{}'", reportFile.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(ReportFrame.this,
                    "Error parsing '" + reportFile.getName() + "'\n" + e.getMessage(), "Parse Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    void saveAllCharts() {
        if (reportPanel != null) {
            GUIFileChooser chooser = new GUIFileChooser(ReportFrame.this.gui, "Select Save Location");
            chooser.setFileSelectionMode(GUIFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(false);

            if (chooser.showDialog(this, "Save") == JFileChooser.APPROVE_OPTION) {
                String directory = chooser.getSelectedFile().getAbsolutePath();

                reportPanel.saveAllCharts(directory);
            }
        }
    }
}
