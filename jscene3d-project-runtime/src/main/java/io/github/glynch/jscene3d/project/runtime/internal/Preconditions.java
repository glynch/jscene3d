/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import java.time.Duration;
import java.util.Objects;

/** Shared project-runtime argument policies. */
public final class Preconditions {
    private Preconditions() {}

    /**
     * Requires text containing at least one non-whitespace character.
     *
     * @param value candidate text
     * @param name argument name used in failure messages
     * @return validated text
     */
    public static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }

    /**
     * Requires a positive duration.
     *
     * @param value candidate duration
     * @param name argument name used in failure messages
     * @return validated duration
     */
    public static Duration requirePositive(Duration value, String name) {
        Duration validValue = requireNonNegative(value, name);
        if (validValue.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return validValue;
    }

    /**
     * Requires a non-negative duration.
     *
     * @param value candidate duration
     * @param name argument name used in failure messages
     * @return validated duration
     */
    public static Duration requireNonNegative(Duration value, String name) {
        Duration validValue = Objects.requireNonNull(value, name);
        if (validValue.isNegative()) {
            throw new IllegalArgumentException(name + " must be non-negative: " + validValue);
        }
        return validValue;
    }

    /**
     * Requires a finite floating-point value in the inclusive unit interval.
     *
     * @param value candidate fraction
     * @param name argument name used in failure messages
     * @return validated fraction
     */
    public static float requireUnitInterval(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be finite and between 0 and 1: " + value);
        }
        return value;
    }
}
