/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies the editor-only minimum splash duration policy. */
final class EditorSplashTimingTest {
    /** Leaves ordinary editor launches unconditionally fast by default. */
    @Test
    void defaultsToNoMinimumVisibility() {
        EditorSplashTiming timing = EditorSplashTiming.fromNamedArguments(Map.of());

        assertThat(timing.minimumVisibility()).isZero();
    }

    /** Accepts whole or fractional seconds from the documented command-line option. */
    @Test
    void readsMinimumSeconds() {
        EditorSplashTiming timing = EditorSplashTiming.fromNamedArguments(Map.of("splash-minimum-seconds", "3.25"));

        assertThat(timing.minimumVisibility()).isEqualTo(Duration.ofMillis(3_250));
    }

    /** Holds only for the unelapsed portion of the configured minimum. */
    @Test
    void calculatesRemainingVisibility() {
        EditorSplashTiming timing = new EditorSplashTiming(Duration.ofSeconds(3));

        assertThat(timing.remainingAfter(Duration.ofMillis(1_250))).isEqualTo(Duration.ofMillis(1_750));
        assertThat(timing.remainingAfter(Duration.ofSeconds(4))).isZero();
    }

    /** Rejects malformed, negative, and non-finite command-line values. */
    @Test
    void rejectsInvalidMinimumSeconds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EditorSplashTiming.fromNamedArguments(Map.of("splash-minimum-seconds", "later")))
                .withMessageContaining("--splash-minimum-seconds");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EditorSplashTiming.fromNamedArguments(Map.of("splash-minimum-seconds", "-1")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EditorSplashTiming.fromNamedArguments(Map.of("splash-minimum-seconds", "NaN")));
    }
}
