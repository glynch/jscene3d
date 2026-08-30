/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Shared renderer argument validation. */
final class Preconditions {
    /** Prevents instantiation of this validation utility class. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /** Requires a non-negative integer argument. */
    static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
        return value;
    }

    /** Requires a positive integer argument. */
    static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
        return value;
    }

    /** Requires a finite floating-point argument in the inclusive unit interval. */
    static float requireUnitInterval(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and between 0 and 1: " + value);
        }
        return value;
    }
}
