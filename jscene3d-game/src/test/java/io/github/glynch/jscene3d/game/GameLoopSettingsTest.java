/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

final class GameLoopSettingsTest {
    @Test
    void buildsImmutableValues() {
        GameLoopSettings first = GameLoopSettings.builder()
                .fixedStep(Duration.ofMillis(10L))
                .maximumFrameTime(Duration.ofMillis(50L))
                .maximumFixedUpdates(5)
                .build();
        GameLoopSettings second = first.toBuilder().build();

        assertThat(first.fixedStep()).isEqualTo(Duration.ofMillis(10L));
        assertThat(first.maximumFrameTime()).isEqualTo(Duration.ofMillis(50L));
        assertThat(first.maximumFixedUpdates()).isEqualTo(5);
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("maximumFixedUpdates=5");
    }

    @Test
    void rejectsInvalidSettings() {
        GameLoopSettings.Builder zeroUpdates = GameLoopSettings.builder();
        GameLoopSettings.Builder reversedDurations =
                GameLoopSettings.builder().fixedStep(Duration.ofMillis(20L)).maximumFrameTime(Duration.ofMillis(10L));

        assertThatThrownBy(() -> zeroUpdates.maximumFixedUpdates(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(reversedDurations::build).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GameLoopSettings.builder().fixedStep(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
