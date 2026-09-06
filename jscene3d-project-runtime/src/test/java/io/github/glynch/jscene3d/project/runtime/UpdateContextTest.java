/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Exercises public fixed and frame timing value contracts. */
final class UpdateContextTest {
    /** Retains valid fixed-update timing exactly. */
    @Test
    void representsFixedUpdateTiming() {
        FixedUpdateContext update = new FixedUpdateContext(3L, Duration.ofMillis(8L), Duration.ofMillis(24L));

        assertThat(update.tick()).isEqualTo(3L);
        assertThat(update.step()).isEqualTo(Duration.ofMillis(8L));
        assertThat(update.simulationTime()).isEqualTo(Duration.ofMillis(24L));
    }

    /** Rejects invalid fixed-update timing. */
    @Test
    void rejectsInvalidFixedUpdateTiming() {
        Duration step = Duration.ofMillis(8L);
        Duration time = Duration.ZERO;

        assertThatThrownBy(() -> new FixedUpdateContext(-1L, step, time))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tick");
        assertThatThrownBy(() -> new FixedUpdateContext(0L, Duration.ZERO, time))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    /** Retains valid frame timing exactly. */
    @Test
    void representsFrameUpdateTiming() {
        FrameUpdateContext update = new FrameUpdateContext(Duration.ofMillis(5L), Duration.ofMillis(16L), 0.5F);

        assertThat(update.elapsed()).isEqualTo(Duration.ofMillis(5L));
        assertThat(update.simulationTime()).isEqualTo(Duration.ofMillis(16L));
        assertThat(update.interpolation()).isEqualTo(0.5F);
    }

    /** Rejects invalid frame-update timing. */
    @Test
    void rejectsInvalidFrameUpdateTiming() {
        Duration negative = Duration.ofNanos(-1L);
        Duration zero = Duration.ZERO;

        assertThatThrownBy(() -> new FrameUpdateContext(negative, zero, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elapsed");
        assertThatThrownBy(() -> new FrameUpdateContext(zero, zero, Float.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 1");
    }
}
