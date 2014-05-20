package com.ibm.nmon.gui.chart.annotate;

import com.ibm.nmon.gui.main.NMONVisualizerGui;
import com.ibm.nmon.gui.GUIDialog;

import com.ibm.nmon.gui.chart.LineChartPanel;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JSpinner.DateEditor;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import com.ibm.nmon.gui.Styles;

public final class LineChartAnnotationDialog extends GUIDialog {
    private static final long serialVersionUID = 6545047405002972062L;

    private final ButtonGroup lineType;

    private final JTextField annotation;

    private final JFormattedTextField yAxisValue;
    // JTextField for value axis or JSpinner for time axis
    private final JFormattedTextField xAxisValue;
    private final JSpinner xAxisTime;

    private boolean useTime;

    private final JCheckBox useYAxisValue;
    private final JCheckBox useXAxisValue;

    private final XYPlot xyPlot;

    public LineChartAnnotationDialog(LineChartPanel lineChartPanel, NMONVisualizerGui gui, JFrame parent,
            Point clickLocation) {
        super(gui, parent, "Annotate Line Chart");

        setLayout(new BorderLayout());
        setModal(true);

        // calculate graph's x, y coordinates from the mouse click position
        xyPlot = lineChartPanel.getChart().getXYPlot();

        addPropertyChangeListener(lineChartPanel);

        java.awt.geom.Rectangle2D dataArea = lineChartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();

        double x = xyPlot.getDomainAxis().java2DToValue(clickLocation.getX(), dataArea, xyPlot.getDomainAxisEdge());
        double y = xyPlot.getRangeAxis().java2DToValue(clickLocation.getY(), dataArea, xyPlot.getRangeAxisEdge());

        if (x < xyPlot.getDomainAxis().getLowerBound()) {
            x = xyPlot.getDomainAxis().getLowerBound();
        }
        if (x > xyPlot.getDomainAxis().getUpperBound()) {
            x = xyPlot.getDomainAxis().getUpperBound();
        }

        if (y < xyPlot.getRangeAxis().getLowerBound()) {
            y = xyPlot.getRangeAxis().getLowerBound();
        }
        if (y > xyPlot.getRangeAxis().getUpperBound()) {
            y = xyPlot.getRangeAxis().getUpperBound();
        }

        useTime = xyPlot.getDomainAxis() instanceof org.jfree.chart.axis.DateAxis;

        // radio buttons at top to select line style
        JLabel lineStyleLabel = new JLabel("Line Style:");
        lineStyleLabel.setFont(Styles.LABEL);
        lineStyleLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        JRadioButton vertical = new JRadioButton("Vertical");
        JRadioButton horizontal = new JRadioButton("Horizontal");
        JRadioButton none = new JRadioButton("None");

        vertical.setActionCommand("Vertical");
        horizontal.setActionCommand("Horizontal");
        none.setActionCommand("None");

        lineType = new ButtonGroup();
        lineType.add(vertical);
        lineType.add(horizontal);
        lineType.add(none);

        // default to vertical line and time annotations
        vertical.setSelected(true);

        // annotation text with ability to set using x or y axis values
        JLabel annotationLabel = new JLabel("Annotation:");
        annotationLabel.setFont(Styles.LABEL);
        annotationLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        annotation = new JTextField();
        annotation.setColumns(5);

        // x axis value as time
        JLabel xAxisLabel = new JLabel("xAxis Location:");
        xAxisLabel.setFont(Styles.LABEL);
        xAxisLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        // for time
        JButton addMinute = null;
        JButton subtractMinute = null;
        JButton roundMinute = null;

        // for values
        JButton roundX = null;
        JButton addOneX = null;
        JButton subtractOneX = null;

        UpdateAnnotationAction annotationUpdater = new UpdateAnnotationAction();

        if (useTime) {
            xAxisTime = new JSpinner(new SpinnerDateModel(new Date((long) x), null, null, Calendar.MINUTE));
            ((DateEditor) xAxisTime.getEditor()).getFormat().setTimeZone(gui.getDisplayTimeZone());

            double range = xyPlot.getDomainAxis().getUpperBound() - xyPlot.getDomainAxis().getLowerBound();

            if (range > (86400 * 1000)) {
                xAxisTime.setEditor(new DateEditor(xAxisTime, Styles.DATE_FORMAT_STRING));
            }
            else {
                xAxisTime.setEditor(new DateEditor(xAxisTime, Styles.DATE_FORMAT_STRING_SHORT));
            }

            xAxisTime.addChangeListener(annotationUpdater);

            xAxisValue = null;

            addMinute = new JButton("Add 1 Min");
            subtractMinute = new JButton("Subtract 1 Min");
            roundMinute = new JButton("Round");

            addMinute.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long time = ((Date) xAxisTime.getValue()).getTime();

                    xAxisTime.setValue(new Date(time + 60 * 1000));
                }
            });

            subtractMinute.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long time = ((Date) xAxisTime.getValue()).getTime();

                    xAxisTime.setValue(new Date(time - 60 * 1000));
                }
            });

            roundMinute.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long time = ((Date) xAxisTime.getValue()).getTime();

                    long mod = time % (60 * 1000);

                    if (mod >= (30 * 1000)) {
                        time += 60 * 1000;
                    }

                    xAxisTime.setValue(new Date(time / (60 * 1000) * (60 * 1000)));
                }
            });
        }
        else {
            xAxisTime = null;
            xAxisValue = new JFormattedTextField(Styles.NUMBER_FORMAT);
            xAxisValue.setValue(x);

            xAxisValue.addPropertyChangeListener(annotationUpdater);

            roundX = new JButton("Round");
            addOneX = new JButton("Add 1");
            subtractOneX = new JButton("Subtract 1");

            roundX.addActionListener(new RoundAction(xAxisValue));
            addOneX.addActionListener(new AddAction(xAxisValue, 1));
            subtractOneX.addActionListener(new AddAction(xAxisValue, -1));
        }

        // y axis value as formatted double
        JLabel yAxisLabel = new JLabel("yAxis Location:");
        yAxisLabel.setFont(Styles.LABEL);
        yAxisLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        yAxisValue = new JFormattedTextField(Styles.NUMBER_FORMAT);
        yAxisValue.setValue(y);

        yAxisValue.addPropertyChangeListener(annotationUpdater);

        JButton roundY = new JButton("Round");
        JButton addOneY = new JButton("Add 1");
        JButton subtractOneY = new JButton("Subtract 1");

        roundY.addActionListener(new RoundAction(yAxisValue));
        addOneY.addActionListener(new AddAction(yAxisValue, 1));
        subtractOneY.addActionListener(new AddAction(yAxisValue, -1));

        useXAxisValue = new JCheckBox("Use xAxis");
        useYAxisValue = new JCheckBox("Use yAxis");

        // OK button at bottom
        JButton ok = new JButton("OK");
        ok.addActionListener(doAnnotation);
        JPanel okPanel = new JPanel();
        okPanel.add(ok);

        // pull question icon from JOptionPane
        JLabel icon = new JLabel((Icon) UIManager.get("OptionPane.questionIcon"));
        icon.setVerticalAlignment(SwingUtilities.TOP);
        icon.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 25));

        JPanel centerPanel = new JPanel(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        GridBagConstraints buttonConstraints = new GridBagConstraints();

        labelConstraints.gridx = 0;
        fieldConstraints.gridx = 1;

        labelConstraints.gridy = 0;
        fieldConstraints.gridy = 0;
        buttonConstraints.gridy = 0;

        labelConstraints.insets = new Insets(5, 0, 5, 2);
        fieldConstraints.insets = new Insets(5, 0, 5, 0);
        buttonConstraints.insets = new Insets(5, 2, 5, 2);

        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        buttonConstraints.fill = GridBagConstraints.HORIZONTAL;

        // labelConstraints.anchor = GridBagConstraints.BASELINE_TRAILING;
        // fieldConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
        buttonConstraints.anchor = GridBagConstraints.CENTER;

        centerPanel.add(lineStyleLabel, labelConstraints);

        buttonConstraints.gridx = 2;
        centerPanel.add(vertical, buttonConstraints);

        ++buttonConstraints.gridx;
        centerPanel.add(horizontal, buttonConstraints);

        ++buttonConstraints.gridx;
        centerPanel.add(none, buttonConstraints);

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;
        ++buttonConstraints.gridy;

        // annotation field is wider than other fields
        fieldConstraints.gridwidth = 2;
        buttonConstraints.gridx = 3;

        centerPanel.add(annotationLabel, labelConstraints);
        centerPanel.add(annotation, fieldConstraints);

        centerPanel.add(useXAxisValue, buttonConstraints);

        ++buttonConstraints.gridx;
        centerPanel.add(useYAxisValue, buttonConstraints);

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;
        ++buttonConstraints.gridy;

        fieldConstraints.gridwidth = 1;
        buttonConstraints.gridx = 2;

        centerPanel.add(xAxisLabel, labelConstraints);

        if (useTime) {
            centerPanel.add(xAxisTime, fieldConstraints);

            centerPanel.add(addMinute, buttonConstraints);

            ++buttonConstraints.gridx;
            centerPanel.add(subtractMinute, buttonConstraints);

            ++buttonConstraints.gridx;
            centerPanel.add(roundMinute, buttonConstraints);

            // the yAxis line will have one more button; make all buttons the same size
            // addOneY.setPreferredSize(subtractMinute.getPreferredSize());
        }
        else {
            centerPanel.add(xAxisValue, fieldConstraints);

            centerPanel.add(addOneX, buttonConstraints);

            ++buttonConstraints.gridx;
            centerPanel.add(subtractOneX, buttonConstraints);

            ++buttonConstraints.gridx;
            centerPanel.add(roundX, buttonConstraints);
        }

        ++labelConstraints.gridy;
        ++fieldConstraints.gridy;
        ++buttonConstraints.gridy;

        buttonConstraints.gridx = 2;

        centerPanel.add(yAxisLabel, labelConstraints);
        centerPanel.add(yAxisValue, fieldConstraints);

        centerPanel.add(addOneY, buttonConstraints);

        ++buttonConstraints.gridx;
        centerPanel.add(subtractOneY, buttonConstraints);

        ++buttonConstraints.gridx;
        centerPanel.add(roundY, buttonConstraints);

        add(centerPanel, BorderLayout.CENTER);
        add(icon, BorderLayout.LINE_START);
        add(okPanel, BorderLayout.PAGE_END);

        useXAxisValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useYAxisValue.setSelected(false);

                if (useXAxisValue.isSelected()) {
                    if (useTime) {
                        annotation.setText(((DateEditor) xAxisTime.getEditor()).getFormat()
                                .format(xAxisTime.getValue()));
                    }
                    else {
                        annotation.setText(xAxisValue.getText());
                    }

                    annotation.setEnabled(false);
                }
                else {
                    annotation.setEnabled(true);
                    annotation.selectAll();
                    annotation.requestFocus();
                }
            }
        });

        useYAxisValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useXAxisValue.setSelected(false);

                if (useYAxisValue.isSelected()) {
                    annotation.setText(yAxisValue.getText());
                    annotation.setEnabled(false);
                }
                else {
                    annotation.setEnabled(true);
                    annotation.selectAll();
                    annotation.requestFocus();
                }
            }
        });

        horizontal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!useYAxisValue.isSelected()) {
                    useYAxisValue.doClick();
                }
            }
        });

        vertical.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!useXAxisValue.isSelected()) {
                    useXAxisValue.doClick();
                }
            }
        });

        none.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (useXAxisValue.isSelected()) {
                    useXAxisValue.doClick();
                }
                if (useYAxisValue.isSelected()) {
                    useYAxisValue.doClick();
                }
            }
        });

        getRootPane().setDefaultButton(ok);

        // set annotation assuming vertical line as the default
        useXAxisValue.doClick();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                // request focus so user can hit spacebar to immediately add a different annotation
                useXAxisValue.requestFocus();
            }
        });
    }

    private ActionListener doAnnotation = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String line = lineType.getSelection().getActionCommand();
            String text = annotation.getText();

            if (text != null) {
                text = text.trim();
            }

            if ("Vertical".equals(line)) {
                ValueMarker marker = new DomainValueMarker(getX());
                marker.setLabelOffset(new RectangleInsets(5, 5, 5, 5));
                marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
                marker.setLabel(text);
                formatMarker(marker);

                if (marker != null) {
                    xyPlot.addDomainMarker(marker);

                    firePropertyChange("annotation", null, marker);
                }
            }
            else if ("Horizontal".equals(line)) {
                ValueMarker marker = new RangeValueMarker(getY());
                marker.setLabelTextAnchor(TextAnchor.BASELINE_LEFT);
                marker.setLabel(text);
                formatMarker(marker);

                if (marker != null) {
                    xyPlot.addRangeMarker(marker);

                    firePropertyChange("annotation", null, marker);
                }
            }
            else if ("None".equals(line)) {
                if (!"".equals(text)) {
                    XYTextAnnotation annotation = new XYTextAnnotation(text, getX(), getY());
                    annotation.setFont(Styles.ANNOTATION_FONT);
                    annotation.setPaint(Styles.ANNOTATION_COLOR);

                    if (annotation != null) {
                        xyPlot.addAnnotation(annotation);

                        firePropertyChange("annotation", null, annotation);
                    }
                }
                // else no annotation needed if no text
            }
            else {
                throw new IllegalStateException("unknown annotation line type");
            }

            dispose();
        }

        private double getX() {
            if (useTime) {
                return ((Date) xAxisTime.getValue()).getTime();
            }
            else {
                return ((Number) xAxisValue.getValue()).doubleValue();
            }
        }

        private double getY() {
            return ((Number) yAxisValue.getValue()).doubleValue();
        }

        private void formatMarker(ValueMarker marker) {
            marker.setStroke(Styles.ANNOTATION_STROKE);
            marker.setPaint(Styles.ANNOTATION_COLOR);
            marker.setLabelFont(Styles.ANNOTATION_FONT);
            marker.setLabelPaint(Styles.ANNOTATION_COLOR);
        }
    };

    private final class UpdateAnnotationAction implements PropertyChangeListener, ChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("value".equals(evt.getPropertyName()) || "editValid".equals(evt.getPropertyName())) {
                if (useXAxisValue.isSelected()) {
                    if (xAxisValue != null) {
                        annotation.setText(xAxisValue.getText());
                    }
                    // else xAxisTime handled by stateChanged()
                }
                else if (useYAxisValue.isSelected()) {
                    annotation.setText(yAxisValue.getText());
                }
                // else do not update the annotation
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            // for xAxisTime
            if (useXAxisValue.isSelected()) {
                annotation.setText(((DateEditor) xAxisTime.getEditor()).getFormat().format(xAxisTime.getValue()));
            }
        }
    }

    // use Number in these functions since JFormattedTextField can return Long or Double
    private final class RoundAction implements ActionListener {
        private final JFormattedTextField textField;

        RoundAction(JFormattedTextField textField) {
            this.textField = textField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long rounded = Math.round(((Number) textField.getValue()).doubleValue());

            textField.setValue((double) rounded);
        }
    }

    private final class AddAction implements ActionListener {
        private final JFormattedTextField textField;
        private final double toAdd;

        AddAction(JFormattedTextField textField, double toAdd) {
            this.textField = textField;
            this.toAdd = toAdd;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            textField.setValue(((Number) textField.getValue()).doubleValue() + toAdd);
        }
    }
}