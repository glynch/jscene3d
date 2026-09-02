/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

/** Shared implementation-only physics argument validation. */
public final class Preconditions {
    /** Prevents instantiation of this validation utility class. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Requires a finite positive floating-point value.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return validated value
     */
    public static float requirePositive(float value, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue <= 0.0F) {
            throw new IllegalArgumentException(parameterName + " must be positive: " + finiteValue);
        }
        return finiteValue;
    }

    /**
     * Requires a finite non-negative floating-point value.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return validated value
     */
    public static float requireNonNegative(float value, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue < 0.0F) {
            throw new IllegalArgumentException(parameterName + " must not be negative: " + finiteValue);
        }
        return finiteValue;
    }

    /**
     * Requires a finite floating-point value.
     *
     * @param value value to validate
     * @param parameterName parameter name used in diagnostics
     * @return validated value
     */
    public static float requireFinite(float value, String parameterName) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(parameterName + " must be finite: " + value);
        }
        return value;
    }
}
