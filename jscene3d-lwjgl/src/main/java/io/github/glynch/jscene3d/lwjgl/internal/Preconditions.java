/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lwjgl.internal;

import java.util.Objects;

/** Shared implementation-only LWJGL argument validation. */
public final class Preconditions {
    /** Prevents instantiation of this validation utility class. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Requires a non-negative integer argument.
     *
     * @param value value to validate
     * @param name argument name used in diagnostics
     * @return validated value
     */
    public static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
        return value;
    }

    /**
     * Requires a positive integer argument.
     *
     * @param value value to validate
     * @param name argument name used in diagnostics
     * @return validated value
     */
    public static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
        return value;
    }

    /**
     * Requires a non-negative finite floating-point argument.
     *
     * @param value value to validate
     * @param name argument name used in diagnostics
     * @return validated value
     */
    public static float requireNonNegative(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0f) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
        return value;
    }

    /**
     * Requires a finite floating-point argument in the inclusive unit interval.
     *
     * @param value value to validate
     * @param name argument name used in diagnostics
     * @return validated value
     */
    public static float requireUnitInterval(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and between 0 and 1: " + value);
        }
        return value;
    }

    /**
     * Requires a finite floating-point argument.
     *
     * @param value value to validate
     * @param name argument name used in diagnostics
     * @return validated value
     */
    public static float requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
        return value;
    }

    /**
     * Requires an ordered finite floating-point interval.
     *
     * @param minimum lower endpoint
     * @param minimumName lower-endpoint name used in diagnostics
     * @param maximum upper endpoint
     * @param maximumName upper-endpoint name used in diagnostics
     */
    public static void requireOrdered(float minimum, String minimumName, float maximum, String maximumName) {
        requireFinite(minimum, minimumName);
        requireFinite(maximum, maximumName);
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    minimumName + " must not exceed " + maximumName + ": " + minimum + " > " + maximum);
        }
    }

    /**
     * Requires a non-blank string argument.
     *
     * @param value value to validate
     * @param name argument name used in diagnostics
     * @return validated value
     */
    public static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }
}
