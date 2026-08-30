/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;

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
}
