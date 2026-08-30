/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

/** Reusable precondition checks for platform arguments. */
final class Preconditions {
    /** Prevents instantiation of this validation utility class. */
    private Preconditions() {}

    /** Requires a positive integer argument. */
    static int requirePositive(int value, String parameterName) {
        if (value <= 0) {
            throw new IllegalArgumentException(parameterName + " must be positive: " + value);
        }
        return value;
    }

    /** Requires a non-negative integer argument. */
    static int requireNonNegative(int value, String parameterName) {
        if (value < 0) {
            throw new IllegalArgumentException(parameterName + " must not be negative: " + value);
        }
        return value;
    }
}
