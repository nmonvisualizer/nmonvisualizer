package com.ibm.nmon.gui.chart;

import java.util.Set;

import java.awt.Font;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.JFreeChart;

import org.jfree.chart.entity.CategoryItemEntity;

import org.jfree.chart.ChartRenderingInfo;

import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.CategoryPlot;

import org.jfree.chart.renderer.category.CategoryItemRenderer;

public final class HighlightableBarChart extends JFreeChart {
    private static final long serialVersionUID = -5389731065593870345L;

    private final Set<CategoryItemEntity> selectedEntities = new java.util.HashSet<CategoryItemEntity>();

    public HighlightableBarChart(Plot plot) {
        super(plot);
    }

    public HighlightableBarChart(String title, Plot plot) {
        super(title, plot);
    }

    public HighlightableBarChart(String title, Font titleFont, Plot plot, boolean createLegend) {
        super(title, titleFont, plot, createLegend);
    }

    void highlightEntity(CategoryItemEntity entity) {
        selectedEntities.add(entity);
    }

    boolean isHighlighted(CategoryItemEntity entity) {
        return selectedEntities.contains(entity);
    }

    public void clearHighlights() {
        selectedEntities.clear();
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D chartArea, Point2D anchor, ChartRenderingInfo info) {
        super.draw(g2, chartArea, anchor, info);

        // for each CategoryItem, highlight the bar
        // this has to be done during painting so it carries over on repaints
        for (CategoryItemEntity entity : selectedEntities) {
            Rectangle2D area = ((Rectangle2D) entity.getArea()).createIntersection(info.getPlotInfo().getDataArea());

            CategoryItemRenderer renderer = ((CategoryPlot) getPlot()).getRenderer();
            int row = entity.getDataset().getRowIndex(entity.getRowKey());
            int column = entity.getDataset().getColumnIndex(entity.getColumnKey());

            java.awt.Color baseColor = (java.awt.Color) renderer.getItemPaint(row, column);

            // redraw the bar with the base color
            // assuming bar was drawn with SimpleGradientBarPainter, this will be a flat color
            g2.setPaint(baseColor);
            g2.fill(area);

            // draw a brighter outline around the bar
            g2.setStroke(renderer.getBaseOutlineStroke());
            g2.setPaint(baseColor.darker());
            g2.draw(area);
        }
    }
}
