/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.internal;

import java.time.Duration;
import java.util.Objects;

/** Shared argument checks for game-runtime values. */
public final class Preconditions {
    /** Prevents instantiation of this validation container. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Returns a positive duration whose nanosecond representation is exact.
     *
     * @param value duration to validate
     * @param name argument name used in failures
     * @return validated duration represented in nanoseconds
     */
    public static Duration requirePositive(Duration value, String name) {
        Duration validValue = Objects.requireNonNull(value, name);
        if (validValue.isZero() || validValue.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + validValue);
        }
        return Duration.ofNanos(validValue.toNanos());
    }

    /**
     * Returns a non-negative duration whose nanosecond representation is exact.
     *
     * @param value duration to validate
     * @param name argument name used in failures
     * @return validated duration represented in nanoseconds
     */
    public static Duration requireNonNegative(Duration value, String name) {
        Duration validValue = Objects.requireNonNull(value, name);
        if (validValue.isNegative()) {
            throw new IllegalArgumentException(name + " must be non-negative: " + validValue);
        }
        return Duration.ofNanos(validValue.toNanos());
    }

    /**
     * Returns a finite inclusive unit-interval value.
     *
     * @param value value to validate
     * @param name argument name used in failures
     * @return validated value
     */
    public static float requireUnitInterval(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be finite and between zero and one: " + value);
        }
        return value;
    }
}
