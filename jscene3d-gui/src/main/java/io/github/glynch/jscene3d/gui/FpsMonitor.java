/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Optional compact frames-per-second overlay with a rolling line graph. */
public final class FpsMonitor implements Overlay {
    private static final int SAMPLE_CAPACITY = 90;
    private static final long DISPLAY_INTERVAL_NANOS = 500_000_000L;
    private static final float WIDTH = 150.0f;
    private static final float HEIGHT = 84.0f;
    private static final float MARGIN = 16.0f;
    private static final float GRAPH_X = MARGIN + 10.0f;
    private static final float GRAPH_Y = MARGIN + 38.0f;
    private static final float GRAPH_WIDTH = WIDTH - 20.0f;
    private static final float GRAPH_HEIGHT = HEIGHT - 48.0f;
    private static final GuiFont FONT = GuiFont.defaultFont();

    private final LongSupplier nanoTime;
    private final GuiTheme theme;
    private final float[] samples = new float[SAMPLE_CAPACITY];
    private final OverlayGuiCanvas overlayCanvas = new OverlayGuiCanvas();

    private boolean visible = true;
    private long previousNanos = Long.MIN_VALUE;
    private long displayIntervalStartNanos;
    private int displayIntervalFrames;
    private int nextSample;
    private int sampleCount;
    private int framesPerSecond;
    private int minimumFramesPerSecond;
    private int maximumFramesPerSecond;

    /** Creates a visible monitor using the default theme and monotonic system clock. */
    public FpsMonitor() {
        this(GuiTheme.dark());
    }

    /**
     * Creates a visible monitor using an explicit theme.
     *
     * @param theme immutable visual theme
     */
    public FpsMonitor(GuiTheme theme) {
        this(System::nanoTime, theme);
    }

    /** Retains an injectable monotonic clock for deterministic tests. */
    FpsMonitor(LongSupplier nanoTime, GuiTheme theme) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    /** Records one presented frame using the monotonic system clock. */
    public void update() {
        update(nanoTime.getAsLong());
    }

    /**
     * Returns whether this monitor is drawn.
     *
     * @return whether this monitor is drawn
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Shows or hides this monitor without discarding samples.
     *
     * @param visible whether this monitor should be visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns the most recently displayed average FPS.
     *
     * @return average FPS, or zero before the first interval completes
     */
    public int framesPerSecond() {
        return framesPerSecond;
    }

    /**
     * Returns the rolling minimum instantaneous FPS.
     *
     * @return rolling minimum, or zero before two frames are recorded
     */
    public int minimumFramesPerSecond() {
        return minimumFramesPerSecond;
    }

    /**
     * Returns the rolling maximum instantaneous FPS.
     *
     * @return rolling maximum, or zero before two frames are recorded
     */
    public int maximumFramesPerSecond() {
        return maximumFramesPerSecond;
    }

    /** Paints the monitor in the upper-left corner. */
    @Override
    public void paint(OverlayCanvas canvas, int width, int height) {
        overlayCanvas.bind(Objects.requireNonNull(canvas, "canvas"));
        try {
            paint(overlayCanvas, width, height);
        } finally {
            overlayCanvas.unbind();
        }
    }

    /** Paints the monitor through the internal headless-testable drawing boundary. */
    void paint(GuiCanvas canvas, int width, int height) {
        Objects.requireNonNull(canvas, "canvas");
        Preconditions.requirePositive(width, "width");
        Preconditions.requirePositive(height, "height");
        if (!visible) {
            return;
        }

        canvas.roundedRectangle(MARGIN + 3.0f, MARGIN + 5.0f, WIDTH, HEIGHT, 8.0f, theme.shadow(), 0.38f);
        canvas.roundedRectangle(MARGIN, MARGIN, WIDTH, HEIGHT, 8.0f, theme.border(), 1.0f);
        canvas.roundedRectangle(MARGIN + 1.0f, MARGIN + 1.0f, WIDTH - 2.0f, HEIGHT - 2.0f, 7.0f, theme.panel(), 0.96f);

        String fps = framesPerSecond + " FPS";
        FONT.text(canvas, MARGIN + 10.0f, MARGIN + 8.0f, fps, 15.0f, theme.text());
        String range = minimumFramesPerSecond + " - " + maximumFramesPerSecond;
        float rangeX = MARGIN + WIDTH - 10.0f - FONT.width(range, 11.0f);
        FONT.text(canvas, rangeX, MARGIN + 11.0f, range, 11.0f, theme.mutedText());

        canvas.roundedRectangle(GRAPH_X, GRAPH_Y, GRAPH_WIDTH, GRAPH_HEIGHT, 4.0f, theme.title(), 0.92f);
        canvas.line(
                GRAPH_X + 4.0f,
                GRAPH_Y + GRAPH_HEIGHT * 0.5f,
                GRAPH_X + GRAPH_WIDTH - 4.0f,
                GRAPH_Y + GRAPH_HEIGHT * 0.5f,
                1.0f,
                theme.border(),
                0.55f);
        paintGraph(canvas);
    }

    /** Records one frame at an explicit monotonic timestamp. */
    void update(long nowNanos) {
        if (previousNanos == Long.MIN_VALUE) {
            previousNanos = nowNanos;
            displayIntervalStartNanos = nowNanos;
            return;
        }
        long frameNanos = nowNanos - previousNanos;
        previousNanos = nowNanos;
        if (frameNanos <= 0L) {
            return;
        }

        samples[nextSample] = 1_000_000_000.0f / frameNanos;
        nextSample = (nextSample + 1) % samples.length;
        sampleCount = Math.min(sampleCount + 1, samples.length);
        displayIntervalFrames++;

        long displayNanos = nowNanos - displayIntervalStartNanos;
        if (displayNanos >= DISPLAY_INTERVAL_NANOS) {
            framesPerSecond = Math.round(displayIntervalFrames * 1_000_000_000.0f / displayNanos);
            updateRange();
            displayIntervalFrames = 0;
            displayIntervalStartNanos = nowNanos;
        }
    }

    /** Paints chronological samples as a continuous antialiased line. */
    private void paintGraph(GuiCanvas canvas) {
        if (sampleCount < 2) {
            return;
        }
        float maximum = Math.max(maximumSample(), 1.0f);
        float previousX = 0.0f;
        float previousY = 0.0f;
        for (int offset = 0; offset < sampleCount; offset++) {
            int sampleIndex = Math.floorMod(nextSample - sampleCount + offset, samples.length);
            float fraction = sampleCount == 1 ? 0.0f : offset / (float) (sampleCount - 1);
            float x = GRAPH_X + 4.0f + fraction * (GRAPH_WIDTH - 8.0f);
            float normalized = Math.clamp(samples[sampleIndex] / maximum, 0.0f, 1.0f);
            float y = GRAPH_Y + GRAPH_HEIGHT - 4.0f - normalized * (GRAPH_HEIGHT - 8.0f);
            if (offset > 0) {
                canvas.line(previousX, previousY, x, y, 1.5f, theme.accent(), 1.0f);
            }
            previousX = x;
            previousY = y;
        }
    }

    /** Recomputes the displayed rolling minimum and maximum. */
    private void updateRange() {
        if (sampleCount == 0) {
            minimumFramesPerSecond = 0;
            maximumFramesPerSecond = 0;
            return;
        }
        float minimum = Float.POSITIVE_INFINITY;
        float maximum = 0.0f;
        for (int index = 0; index < sampleCount; index++) {
            minimum = Math.min(minimum, samples[index]);
            maximum = Math.max(maximum, samples[index]);
        }
        minimumFramesPerSecond = Math.round(minimum);
        maximumFramesPerSecond = Math.round(maximum);
    }

    /** Returns the largest rolling sample for graph normalization. */
    private float maximumSample() {
        float maximum = 0.0f;
        for (int index = 0; index < sampleCount; index++) {
            maximum = Math.max(maximum, samples[index]);
        }
        return maximum;
    }
}
