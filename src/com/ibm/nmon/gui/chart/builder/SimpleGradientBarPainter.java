package com.ibm.nmon.gui.chart.builder;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.RectangularShape;

import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.ui.RectangleEdge;

/**
 * Draws bars with a simple gradient that gets 25% darker across the bar to provide a simple 3d
 * shade effect.
 */
final class SimpleGradientBarPainter extends StandardBarPainter {
    @Override
    public void paintBar(Graphics2D g2, BarRenderer renderer, int row, int column, RectangularShape bar,
            RectangleEdge base) {

        Paint itemPaint = renderer.getItemPaint(row, column);

        Color c0, c1;

        if (itemPaint instanceof Color) {
            c0 = (Color) itemPaint;
            // 1/4 darker
            c1 = new Color(Math.max((int) (c0.getRed() * .75f), 0), Math.max((int) (c0.getGreen() * .75f), 0), Math
                    .max((int) (c0.getBlue() * .75f), 0));
        }
        else if (itemPaint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) itemPaint;
            c0 = gp.getColor1();
            c1 = gp.getColor2();
        }
        else {
            c0 = Color.BLUE;
            c1 = c0.darker();
        }

        // if the bar color has alpha == 0, do not draw anything
        if (c0.getAlpha() == 0) {
            return;
        }

        GradientPaint gp = null;

        if (base == RectangleEdge.TOP || base == RectangleEdge.BOTTOM) {
            // gradient left to right - right edge darker
            gp = new GradientPaint((float) bar.getMinX(), 0, c0, (float) bar.getMaxX(), 0, c1);

        }
        else if (base == RectangleEdge.LEFT || base == RectangleEdge.RIGHT) {
            // gradient top to bottom - bottom edge darker
            gp = new GradientPaint(0, (float) bar.getMinY(), c0, 0, (float) bar.getMaxY(), c1);
        }

        g2.setPaint(gp);
        g2.fill(bar);

        if (renderer.isDrawBarOutline()) {
            Stroke stroke = renderer.getItemOutlineStroke(row, column);
            Paint paint = renderer.getItemOutlinePaint(row, column);

            if (stroke != null && paint != null) {
                g2.setStroke(stroke);
                g2.setPaint(paint);
                g2.draw(bar);
            }
        }
    }
}
