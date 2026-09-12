/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** Controls minimum cold-start splash visibility without delaying editor work. */
record EditorSplashTiming(Duration startupMinimumVisibility) {
    static final String MINIMUM_SECONDS_ARGUMENT = "splash-minimum-seconds";
    private static final Duration DEFAULT_STARTUP_MINIMUM_VISIBILITY = Duration.ofSeconds(2);

    /** Validates a minimum visibility duration. */
    EditorSplashTiming {
        Objects.requireNonNull(startupMinimumVisibility, "startupMinimumVisibility");
        if (startupMinimumVisibility.isNegative()) {
            throw new IllegalArgumentException("startupMinimumVisibility must not be negative");
        }
    }

    /** Reads the editor-only splash timing option from JavaFX named arguments. */
    static EditorSplashTiming fromNamedArguments(Map<String, String> namedArguments) {
        String seconds =
                Objects.requireNonNull(namedArguments, "namedArguments").get(MINIMUM_SECONDS_ARGUMENT);
        if (seconds == null) {
            return new EditorSplashTiming(DEFAULT_STARTUP_MINIMUM_VISIBILITY);
        }
        return new EditorSplashTiming(parseSeconds(seconds));
    }

    /** Returns how much longer the active presentation must remain after the supplied elapsed time. */
    Duration remainingAfter(EditorSplashPresentation presentation, Duration elapsed) {
        Objects.requireNonNull(presentation, "presentation");
        Duration validElapsed = Objects.requireNonNull(elapsed, "elapsed");
        if (validElapsed.isNegative()) {
            throw new IllegalArgumentException("elapsed must not be negative");
        }
        if (presentation == EditorSplashPresentation.PROJECT_LOADING) {
            return Duration.ZERO;
        }
        Duration remaining = startupMinimumVisibility.minus(validElapsed);
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
