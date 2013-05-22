package com.ibm.nmon.gui.chart;

import java.awt.Stroke;
import java.awt.BasicStroke;

import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartMouseEvent;

import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;

import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.CategoryDataset;

import org.jfree.chart.renderer.category.LineAndShapeRenderer;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

public final class IntervalChartPanel extends BaseChartPanel implements ChartMouseListener {
    private final BasicStroke SELECTED_STROKE = new BasicStroke(5);

    public IntervalChartPanel(NMONVisualizerGui gui) {
        super(gui);

        addChartMouseListener(this);
    }

    // note that these methods assume the chart contains a CategoryPlot rendered by a
    // LineAndShapeRenderer
    // these are the expected values for all charts created by IntervalChartBuilder
    public void highlightElement(int row, int column) {
        if (getChart() != null) {
            CategoryDataset dataset = ((CategoryPlot) getChart().getPlot()).getDataset();

            if ((row >= 0) && (row < dataset.getRowCount())) {
                LineAndShapeRenderer renderer = (LineAndShapeRenderer) ((CategoryPlot) getChart().getPlot())
                        .getRenderer();
                renderer.setSeriesStroke(row, SELECTED_STROKE);
            }
        }
    }

    @Override
    public final void clearHighlightedElements() {
        if (getChart() != null) {
            ((LineAndShapeRenderer) ((CategoryPlot) getChart().getPlot()).getRenderer()).clearSeriesStrokes(false);
        }
    }

    @Override
    public void setElementVisible(int row, int column, boolean visible) {
        if (getChart() != null) {
            CategoryPlot plot = (CategoryPlot) getChart().getPlot();
            if ((row >= 0) && (row < plot.getDataset().getRowCount())) {
                plot.getRenderer().setSeriesVisible(row, visible);
            }
        }
    }

    @Override
    protected String getSaveFileName() {
        String title = getChart().getTitle().getText();

        if ((title == null) || "".equals(title)) {
            return "chart_" + this.hashCode() + " by Interval";
        }
        else {
            return title + " by Interval";
        }
    }

    @Override
    public final void chartMouseClicked(ChartMouseEvent event) {
        CategoryItemEntity entity = null;
        int series = -1;

        // users can click on either the line or the legend
        // regardless, figure out the series index and the associated category
        if (event.getEntity().getClass() == CategoryItemEntity.class) {
            entity = (CategoryItemEntity) event.getEntity();
            series = entity.getDataset().getRowIndex(entity.getRowKey());
        }
        else if (event.getEntity().getClass() == LegendItemEntity.class) {
            LegendItemEntity legendEntity = (LegendItemEntity) event.getEntity();
            CategoryDataset dataset = (CategoryDataset) legendEntity.getDataset();

            series = dataset.getRowIndex(legendEntity.getSeriesKey());

            // find the CategoryItemEntity that matches the given legend's series
            // assume there are not that many entities in the chart and this will be relatively fast
            @SuppressWarnings("rawtypes")
            java.util.Iterator i = getChartRenderingInfo().getEntityCollection().iterator();

            while (i.hasNext()) {
                ChartEntity e = (ChartEntity) i.next();

                if (e.getClass() == CategoryItemEntity.class) {
                    CategoryItemEntity categoryEntity = (CategoryItemEntity) e;

                    if (categoryEntity.getRowKey().equals(legendEntity.getSeriesKey())) {
                        entity = categoryEntity;
                        break;
                    }
                }
            }
        }

        if (entity != null) {
            LineAndShapeRenderer renderer = (LineAndShapeRenderer) ((CategoryPlot) getChart().getPlot()).getRenderer();
            Stroke oldHighlight = renderer.getSeriesStroke(series);

            // clear existing highlights
            renderer.clearSeriesStrokes(false);

            // toggle series stroke
            if (oldHighlight != SELECTED_STROKE) {
                renderer.setSeriesStroke(series, SELECTED_STROKE);
                firePropertyChange("highlightedIntervalLine", null, series);
            }
            else {
                renderer.setSeriesStroke(series, null);
                firePropertyChange("highlightedIntervalLine", series, null);
            }

            // assume whatever fired the event will repaint the chart
        }
    }

    @Override
    public final void chartMouseMoved(ChartMouseEvent event) {}
}
