package com.ibm.nmon.gui.chart;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartMouseEvent;

import org.jfree.chart.annotations.Annotation;
import org.jfree.chart.annotations.CategoryTextAnnotation;

import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.CategoryItemEntity;

import org.jfree.chart.plot.CategoryPlot;

import com.ibm.nmon.gui.Styles;

import com.ibm.nmon.gui.chart.data.DataTupleCategoryDataset;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

public final class BarChartPanel extends BaseChartPanel implements ChartMouseListener {
    private static final long serialVersionUID = -5854445408091165201L;

    private final JMenuItem annotateBar;
    private boolean canAnnotate = false;

    public BarChartPanel(NMONVisualizerGui gui) {
        super(gui);

        addChartMouseListener(this);

        annotateBar = new JMenuItem("Annotate Bar");
        annotateBar.addActionListener(new AnnotateBarAction());
    }

    @Override
    public void highlightElement(int row, int column) {
        if ((getChart() != null) && (getChart().getClass() == HighlightableBarChart.class)) {
            boolean highlightWholeBar = ((DataTupleCategoryDataset) getChart().getCategoryPlot().getDataset())
                    .categoriesHaveDifferentStats();

            // find the CategoryItemEntity that matches the given row and column
            // assume there are not that many entities in the chart and this will be relatively fast
            @SuppressWarnings("rawtypes")
            java.util.Iterator i = getChartRenderingInfo().getEntityCollection().iterator();

            while (i.hasNext()) {
                ChartEntity entity = (ChartEntity) i.next();

                if (entity.getClass() == CategoryItemEntity.class) {
                    CategoryItemEntity categoryEntity = (CategoryItemEntity) entity;

                    int currentRow = categoryEntity.getDataset().getRowIndex(categoryEntity.getRowKey());
                    int currentColumn = categoryEntity.getDataset().getColumnIndex(categoryEntity.getColumnKey());

                    if (highlightWholeBar) { // match all categories
                        if (currentColumn == column) {
                            ((HighlightableBarChart) getChart()).highlightEntity(categoryEntity);
                        }
                    }
                    else { // match only a single category
                        if ((currentRow == row) && (currentColumn == column)) {
                            ((HighlightableBarChart) getChart()).highlightEntity(categoryEntity);

                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void clearHighlightedElements() {
        if ((getChart() != null) && (getChart().getClass() == HighlightableBarChart.class)) {
            ((HighlightableBarChart) getChart()).clearHighlights();

            // force a repaint of the double buffered chart
            setRefreshBuffer(true);
            repaint();
        }
    }

    @Override
    public void clearChart() {
        super.clearChart();

        if (getChart() != null) {
            removeAnnotationMenu();
        }
    }

    @Override
    public void addAnnotations(List<Annotation> annotations) {
        for (Annotation a : annotations) {
            if (a instanceof CategoryTextAnnotation) {
                CategoryTextAnnotation annotation = (CategoryTextAnnotation) a;
                getChart().getCategoryPlot().addAnnotation(annotation);
            }
        }
    }

    @Override
    protected void displayPopupMenu(int x, int y) {
        // only show annotation menu if the mouse if over an actual bar
        // find the CategoryItemEntity that matches the given x, y
        // assume there are not that many entities in the chart and this will be relatively
        // fast
        @SuppressWarnings("rawtypes")
        java.util.Iterator i = getChartRenderingInfo().getEntityCollection().iterator();

        boolean valid = false;

        while (i.hasNext()) {
            ChartEntity entity = (ChartEntity) i.next();

            if (entity.getClass() == CategoryItemEntity.class) {
                CategoryItemEntity categoryEntity = (CategoryItemEntity) entity;

                if (categoryEntity.getArea().contains(x, y)) {
                    ((AnnotateBarAction) annotateBar.getActionListeners()[0]).categoryKey = (String) categoryEntity
                            .getColumnKey();
                    valid = true;
                    break;
                }
            }
        }

        if (valid) {
            if (!canAnnotate) {
                getPopupMenu().addSeparator();
                getPopupMenu().add(annotateBar);

                canAnnotate = true;
            }
            // else menu already present
        }
        else {
            removeAnnotationMenu();
        }

        super.displayPopupMenu(x, y);
    }

    protected final void removeAnnotationMenu() {
        if (!canAnnotate) {
            boolean removed = false;

            for (java.awt.Component c : getPopupMenu().getComponents()) {
                if (c == annotateBar) {
                    getPopupMenu().remove(annotateBar);
                    removed = true;
                    break;
                }
            }

            if (removed) {
                // remove separator
                getPopupMenu().remove(getPopupMenu().getComponentCount() - 1);
            }
        }

        canAnnotate = false;
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        if (event.getEntity().getClass() == CategoryItemEntity.class) {
            if ((getChart() != null) && (getChart().getClass() == HighlightableBarChart.class)) {
                HighlightableBarChart chart = (HighlightableBarChart) getChart();
                CategoryItemEntity entity = (CategoryItemEntity) event.getEntity();

                // toggle highlight if already selected
                if (chart.isHighlighted(entity)) {
                    chart.clearHighlights();

                    firePropertyChange("highlightedBar", getRowAndColumn(entity), null);
                }
                else {
                    chart.clearHighlights();
                    chart.highlightEntity(entity);

                    firePropertyChange("highlightedBar", null, getRowAndColumn(entity));
                }

                // assume whatever fired the event will repaint the chart
            }
        }
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {}

    private int[] getRowAndColumn(CategoryItemEntity entity) {
        int[] toReturn = new int[2];

        toReturn[0] = entity.getDataset().getRowIndex(entity.getRowKey());
        toReturn[1] = entity.getDataset().getColumnIndex(entity.getColumnKey());

        return toReturn;
    }

    private final class AnnotateBarAction implements ActionListener {
        String categoryKey = null;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (categoryKey == null) {
                return;
            }

            String text = JOptionPane.showInputDialog(gui.getMainFrame(), "Annotation Text", "Annotate Bar Chart",
                    JOptionPane.QUESTION_MESSAGE);

            if (text != null) {
                text = text.trim();

                if ("".equals(text)) {
                    return;
                }

                CategoryPlot categoryPlot = getChart().getCategoryPlot();

                double y = categoryPlot.getRangeAxis().java2DToValue(clickLocation.getY(),
                        getChartRenderingInfo().getPlotInfo().getDataArea(), categoryPlot.getRangeAxisEdge());

                if (y < categoryPlot.getRangeAxis().getLowerBound()) {
                    y = categoryPlot.getRangeAxis().getLowerBound();
                }
                if (y > categoryPlot.getRangeAxis().getUpperBound()) {
                    y = categoryPlot.getRangeAxis().getUpperBound();
                }

                CategoryTextAnnotation annotation = new CategoryTextAnnotation(text, categoryKey, y);
                annotation.setFont(Styles.ANNOTATION_FONT);
                annotation.setPaint(Styles.ANNOTATION_COLOR);

                getChart().getCategoryPlot().addAnnotation(annotation);

                firePropertyChange("annotation", null, annotation);
            }
        }
    }
}
