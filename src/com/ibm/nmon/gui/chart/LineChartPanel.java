package com.ibm.nmon.gui.chart;

import java.awt.Stroke;
import java.awt.BasicStroke;

import java.beans.PropertyChangeEvent;

import java.util.TimeZone;

import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartMouseEvent;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import org.jfree.data.xy.XYDataset;

import org.jfree.chart.axis.DateAxis;

import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.entity.LegendItemEntity;

import com.ibm.nmon.gui.chart.builder.LineChartBuilder;
import com.ibm.nmon.gui.main.NMONVisualizerGui;

public class LineChartPanel extends BaseChartPanel implements ChartMouseListener {
    private final BasicStroke SELECTED_STROKE = new BasicStroke(5);

    public LineChartPanel(NMONVisualizerGui gui) {
        super(gui);

        addChartMouseListener(this);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) {
            if (enabled) {
                gui.addPropertyChangeListener("timeZone", this);
                gui.addPropertyChangeListener("chartRelativeTime", this);

                setAxisTimeZone(gui.getDisplayTimeZone());
                setRelativeAxis(gui.getBooleanProperty("chartRelativeTime"));
            }
            else {
                gui.removePropertyChangeListener("timeZone", this);
                gui.removePropertyChangeListener("chartRelativeTime", this);
            }

            super.setEnabled(enabled);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("chartRelativeTime".equals(evt.getPropertyName())) {
            setRelativeAxis((Boolean) evt.getNewValue());
        }
        else if ("timeZone".equals(evt.getPropertyName())) {
            setAxisTimeZone((TimeZone) evt.getNewValue());
        }
    }

    // note that these methods assume the chart contains an XYPlot rendered by an XYItemRenderer
    // these are the expected values for all charts created by LineChartBuilder
    public final void highlightElement(int row, int column) {
        if (getChart() != null) {
            if ((row >= 0) && (row < getChart().getXYPlot().getDataset().getSeriesCount())) {
                XYItemRenderer renderer = getChart().getXYPlot().getRenderer();
                renderer.setSeriesStroke(row, SELECTED_STROKE);
            }
        }
    }

    @Override
    public final void clearHighlightedElements() {
        if (getChart() != null) {
            ((AbstractRenderer) getChart().getXYPlot().getRenderer()).clearSeriesStrokes(false);
        }
    }

    @Override
    public void setElementVisible(int row, int column, boolean visible) {
        if (getChart() != null) {
            if ((row >= 0) && (row < getChart().getXYPlot().getDataset().getSeriesCount())) {
                getChart().getXYPlot().getRenderer().setSeriesVisible(row, visible);
            }
        }
    }

    @Override
    public final void chartMouseClicked(ChartMouseEvent event) {
        int series = -1;

        ChartEntity entity = event.getEntity();

        if (entity == null) {
            return;
        }

        // users can click on either the line or the legend
        // regardless, figure out the series index
        if (entity.getClass() == XYItemEntity.class) {
            series = ((XYItemEntity) event.getEntity()).getSeriesIndex();
        }
        else if (entity.getClass() == LegendItemEntity.class) {
            LegendItemEntity legendEntity = (LegendItemEntity) event.getEntity();
            XYDataset dataset = (XYDataset) legendEntity.getDataset();

            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                if (dataset.getSeriesKey(i).equals(legendEntity.getSeriesKey())) {
                    series = i;
                    break;
                }
            }
        }

        if (series != -1) {
            XYItemRenderer renderer = getChart().getXYPlot().getRenderer();
            Stroke oldHighlight = renderer.getSeriesStroke(series);

            // clear existing highlights
            ((AbstractRenderer) getChart().getXYPlot().getRenderer()).clearSeriesStrokes(false);

            // toggle series stroke
            if (oldHighlight != SELECTED_STROKE) {
                renderer.setSeriesStroke(series, SELECTED_STROKE);

                firePropertyChange("highlightedLine", null, series);
            }
            else {
                renderer.setSeriesStroke(series, null);

                firePropertyChange("highlightedLine", series, null);
            }

            // assume whatever fired the event will repaint the chart
        }
    }

    @Override
    public final void chartMouseMoved(ChartMouseEvent event) {}

    private void setAxisTimeZone(TimeZone timeZone) {
        if (getChart() != null) {
            XYPlot plot = getChart().getXYPlot();

            if (plot.getDomainAxis() instanceof DateAxis) {
                ((DateAxis) plot.getDomainAxis()).setTimeZone(timeZone);
            }
        }
    }

    private void setRelativeAxis(boolean relative) {
        if (relative) {
            LineChartBuilder.setRelativeAxis(getChart(), gui.getMinSystemTime());
        }
        else {
            LineChartBuilder.setAbsoluteAxis(getChart());
        }
    }
}
