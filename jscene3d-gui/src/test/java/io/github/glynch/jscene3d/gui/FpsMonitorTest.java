/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.gui.internal.GuiCanvas;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class FpsMonitorTest {
    @Test
    void reportsAverageAndRollingRangeAfterDisplayInterval() {
        FpsMonitor monitor = new FpsMonitor(() -> 0L, GuiTheme.dark());

        monitor.update(0L);
        for (int frame = 1; frame <= 30; frame++) {
            monitor.update(frame * 16_666_667L);
        }

        assertThat(monitor.framesPerSecond()).isEqualTo(60);
        assertThat(monitor.minimumFramesPerSecond()).isEqualTo(60);
        assertThat(monitor.maximumFramesPerSecond()).isEqualTo(60);
    }

    @Test
    void retainsMeasurementsWhileHidden() {
        FpsMonitor monitor = new FpsMonitor(() -> 0L, GuiTheme.dark());
        monitor.setVisible(false);

        monitor.update(1L);
        monitor.update(1_000_000_001L);

        assertThat(monitor.isVisible()).isFalse();
        assertThat(monitor.framesPerSecond()).isEqualTo(1);
    }

    @Test
    void publicUpdateUsesInjectedMonotonicClock() {
        AtomicLong clock = new AtomicLong();
        FpsMonitor monitor = new FpsMonitor(clock::get, GuiTheme.dark());

        monitor.update();
        clock.set(1_000_000_000L);
        monitor.update();

        assertThat(monitor.framesPerSecond()).isEqualTo(1);
    }

    @Test
    void ignoresNonIncreasingTimestamps() {
        FpsMonitor monitor = new FpsMonitor(() -> 0L, GuiTheme.dark());

        monitor.update(10L);
        monitor.update(10L);
        monitor.update(9L);

        assertThat(monitor.framesPerSecond()).isZero();
        assertThat(monitor.minimumFramesPerSecond()).isZero();
        assertThat(monitor.maximumFramesPerSecond()).isZero();
    }

    @Test
    void paintsFrameStatisticsAndRollingGraph() {
        FpsMonitor monitor = new FpsMonitor(() -> 0L, GuiTheme.dark());
        monitor.update(0L);
        for (int frame = 1; frame <= 100; frame++) {
            monitor.update(frame * 16_666_667L);
        }
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        monitor.paint(canvas, 800, 600);

        assertThat(canvas.roundedRectangleCount()).isEqualTo(4);
        assertThat(canvas.lineCount()).isEqualTo(90);
        assertThat(canvas.alphaMaskCount()).isPositive();
    }

    @Test
    void paintsNoGraphSegmentsBeforeTwoSamples() {
        FpsMonitor monitor = new FpsMonitor(() -> 0L, GuiTheme.dark());
        monitor.update(0L);
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        monitor.paint(canvas, 800, 600);

        assertThat(canvas.lineCount()).isOne();
    }

    @Test
    void hiddenMonitorDoesNotPaint() {
        FpsMonitor monitor = new FpsMonitor(() -> 0L, GuiTheme.dark());
        monitor.setVisible(false);
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        monitor.paint(canvas, 800, 600);

        assertThat(canvas.commandCount()).isZero();
    }

    @Test
    void acceptsFiniteCustomPosition() {
        FpsMonitor monitor = new FpsMonitor(() -> 0L, GuiTheme.dark());
        monitor.setPosition(348.0f, 16.0f);
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        monitor.paint(canvas, 1000, 720);

        assertThat(canvas.commandCount()).isPositive();
        assertThatIllegalArgumentException().isThrownBy(() -> monitor.setPosition(Float.NaN, 0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> monitor.setPosition(0.0f, Float.POSITIVE_INFINITY));
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void validatesConstructionAndPaintDimensions() {
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();
        FpsMonitor monitor = new FpsMonitor(() -> 0L, GuiTheme.dark());

        assertThatNullPointerException().isThrownBy(() -> new FpsMonitor(null, GuiTheme.dark()));
        assertThatNullPointerException().isThrownBy(() -> new FpsMonitor(() -> 0L, null));
        assertThatNullPointerException().isThrownBy(() -> monitor.paint((GuiCanvas) null, 800, 600));
        assertThatIllegalArgumentException().isThrownBy(() -> monitor.paint(canvas, 0, 600));
        assertThatIllegalArgumentException().isThrownBy(() -> monitor.paint(canvas, 800, 0));
    }
}
