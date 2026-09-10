/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Controls optional minimum splash visibility without delaying editor work. */
record EditorSplashTiming(Duration minimumVisibility) {
    static final String MINIMUM_SECONDS_ARGUMENT = "splash-minimum-seconds";

    /** Validates a minimum visibility duration. */
    EditorSplashTiming {
        Objects.requireNonNull(minimumVisibility, "minimumVisibility");
        if (minimumVisibility.isNegative()) {
            throw new IllegalArgumentException("minimumVisibility must not be negative");
        }
    }

    /** Reads the editor-only splash timing option from JavaFX named arguments. */
    static EditorSplashTiming fromNamedArguments(Map<String, String> namedArguments) {
        String seconds =
                Objects.requireNonNull(namedArguments, "namedArguments").getOrDefault(MINIMUM_SECONDS_ARGUMENT, "0");
        return new EditorSplashTiming(parseSeconds(seconds));
    }

    /** Returns how much longer the splash must remain after the supplied elapsed time. */
    Duration remainingAfter(Duration elapsed) {
        Duration validElapsed = Objects.requireNonNull(elapsed, "elapsed");
        if (validElapsed.isNegative()) {
            throw new IllegalArgumentException("elapsed must not be negative");
        }
        Duration remaining = minimumVisibility.minus(validElapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /** Parses a finite, non-negative duration expressed in seconds. */
    private static Duration parseSeconds(String value) {
        try {
            double seconds = Double.parseDouble(value);
            if (!Double.isFinite(seconds) || seconds < 0.0) {
                throw invalidValue(value);
            }
            return Duration.ofNanos(Math.round(seconds * 1_000_000_000.0));
        } catch (NumberFormatException exception) {
            throw invalidValue(value, exception);
        }
    }

    /** Creates a consistent command-line validation error. */
    private static IllegalArgumentException invalidValue(String value) {
        return new IllegalArgumentException(
                "--" + MINIMUM_SECONDS_ARGUMENT + " must be a finite, non-negative number: " + value);
    }

    /** Creates a consistent command-line validation error retaining its parsing cause. */
    private static IllegalArgumentException invalidValue(String value, NumberFormatException cause) {
        return new IllegalArgumentException(
                "--" + MINIMUM_SECONDS_ARGUMENT + " must be a finite, non-negative number: " + value, cause);
    }
}
