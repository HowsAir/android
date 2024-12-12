package com.example.mborper.breathbetter.graphs;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Custom renderer for BarChart to draw bars with rounded corners.
 * This renderer overrides the default rendering behavior to provide custom bar shapes with rounded corners,
 * enhancing the visual appearance of the bars in the chart.
 * <p>
 * The rendering is done by modifying the drawing of the bars to create rounded rectangles
 * instead of the default rectangular bars.
 *
 * @author Alejandro Rosado
 * @since  2024-11-20
 * last edited: 2024-12-12
 */
public class RoundedBarChartRenderer extends BarChartRenderer {

    private final float cornerRadius;

    /**
     * Constructor to initialize the RoundedBarChartRenderer with a given corner radius for the bars.
     * <p>
     * Initializes the renderer with the specified corner radius, which will be used to round the corners of the bars.
     *
     * @param chart The BarChart instance for which the renderer is being created
     * @param cornerRadius The radius of the corners for the bars
     */
    public RoundedBarChartRenderer(BarChart chart, float cornerRadius) {
        super(chart, chart.getAnimator(), chart.getViewPortHandler());
        this.cornerRadius = cornerRadius;
    }

    /**
     * Overrides the default drawValues method to prevent drawing values on top of the bars.
     * <p>
     * The method is intentionally left empty to avoid displaying values on the chart,
     * as the focus is on rendering the bars with rounded corners.
     *
     * @param c The canvas on which the values would be drawn (if this method were not overridden)
     */
    @Override
    public void drawValues(Canvas c) {
        // Override to prevent drawing values
        // Do nothing
    }

    /**
     * Draws the dataset (bars) with rounded corners on the chart.
     * <p>
     * This method overrides the default drawing behavior of BarChartRenderer to draw each bar as a rounded rectangle
     * with the specified corner radius, making the bars appear with rounded corners rather than sharp edges.
     *
     * @param canvas The canvas to draw the bars on
     * @param dataSet The dataset containing the bar entries to be drawn
     * @param index The index of the dataset being drawn
     */
    @Override
    public void drawDataSet(Canvas canvas, IBarDataSet dataSet, int index) {
        if (dataSet == null || dataSet.getEntryCount() == 0) {
            return;
        }

        Transformer transformer = mChart.getTransformer(dataSet.getAxisDependency());
        float barWidth = mChart.getBarData().getBarWidth();
        float barWidthHalf = barWidth / 2f;

        mRenderPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < dataSet.getEntryCount(); i++) {
            BarEntry entry = dataSet.getEntryForIndex(i);

            // Set paint color for each bar
            mRenderPaint.setColor(dataSet.getColor(i));

            // Prepare bar coordinates
            float left = entry.getX() - barWidthHalf;
            float right = entry.getX() + barWidthHalf;
            float bottom = entry.getY();
            float top = 0f;

            // Transform coordinates
            float[] pts = {left, bottom, right, top};
            transformer.pointValuesToPixel(pts);

            // Create rectangular bounds
            RectF barRect = new RectF(pts[0], pts[3], pts[2], pts[1]);

            // Draw rounded rectangle
            Path path = new Path();
            path.addRoundRect(barRect,
                    new float[]{
                            cornerRadius, cornerRadius,   // Top left
                            cornerRadius, cornerRadius,   // Top right
                            0f, 0f,                       // Bottom right (no rounding)
                            0f, 0f                        // Bottom left (no rounding)
                    },
                    Path.Direction.CW);

            canvas.drawPath(path, mRenderPaint);
        }
    }
}