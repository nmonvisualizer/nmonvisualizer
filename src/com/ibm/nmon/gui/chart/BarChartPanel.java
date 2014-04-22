package com.ibm.nmon.gui.chart;

import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartMouseEvent;

import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.CategoryItemEntity;

import com.ibm.nmon.gui.chart.data.DataTupleCategoryDataset;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

public final class BarChartPanel extends BaseChartPanel implements ChartMouseListener {
    private static final long serialVersionUID = -5854445408091165201L;

    public BarChartPanel(NMONVisualizerGui gui) {
        super(gui);

        addChartMouseListener(this);
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
}
