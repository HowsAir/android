package com.example.mborper.breathbetter.graphs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.mborper.breathbetter.R;

/**
 * Custom View that draws a semicircular progress bar.
 * <p>
 * This view is used to display a semicircular progress indicator, such as for showing
 * the progress of a task or a value represented as a percentage.
 *
 * @author Alejandro Rosado
 * @since 2024-12-11
 * last edited: 2024-12-12
 */
public class SemicircleProgressView extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF oval;
    private float progress = 0;

    /**
     * Constructor for SemicircleProgressView.
     * <p>
     * Initializes the view by calling the init() method to set up paints and other properties.
     *
     * @param context The context associated with this view.
     * @param attrs   The set of attributes from XML that can be used to configure this view.
     */
    public SemicircleProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initializes the paint objects and other properties for drawing the semicircular progress bar.
     * <p>
     * Sets up the background and progress paints with specific colors, stroke widths, and caps.
     * Defines an oval rectangle for drawing the semicircle.
     */
    private void init() {
        // Background paint (light gray)
        backgroundPaint = new Paint();
        backgroundPaint.setColor(getResources().getColor(R.color.gray));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(70f);
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        // Progress paint (primary color)
        progressPaint = new Paint();
        progressPaint.setColor(getResources().getColor(R.color.primary));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(70f);
        progressPaint.setAntiAlias(true);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // Oval for drawing
        oval = new RectF();
    }

    /**
     * Called when the size of the view changes (e.g., during layout).
     * <p>
     * This method calculates the bounds of the oval (which defines the semicircle)
     * based on the current size of the view.
     *
     * @param w     The new width of the view
     * @param h     The new height of the view
     * @param oldw  The previous width of the view
     * @param oldh  The previous height of the view
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Create the oval for the semicircle
        float padding = 50f;
        oval.set(
                padding,
                padding,
                w - padding,
                (h * 2) - padding
        );
    }

    /**
     * Draws the semicircle background and the progress portion of the semicircle.
     * <p>
     * This method draws two arcs: the background arc (the full semicircle) and the progress arc
     * (which is a portion of the semicircle based on the progress value).
     *
     * @param canvas The canvas on which to draw the arcs
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw full semicircle background
        canvas.drawArc(oval, 180, 180, false, backgroundPaint);

        // Draw progress semicircle
        float sweepAngle = (progress / 100f) * 180;
        canvas.drawArc(oval, 180, sweepAngle, false, progressPaint);
    }

    /**
     * Sets the progress value for the semicircular progress bar.
     * <p>
     * The value should be between 0 and 100. Any value outside this range will be clamped to
     * the nearest valid value.
     *
     * @param progress The progress value between 0 and 100.
     */
    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate(); // Redraw the view
    }
}