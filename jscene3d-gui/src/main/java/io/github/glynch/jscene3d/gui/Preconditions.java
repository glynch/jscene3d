/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import java.util.Objects;

/** Shared GUI argument validation. */
final class Preconditions {
    /** Prevents instantiation of this validation utility class. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /** Requires a positive integer argument. */
    static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
        return value;
    }

    /** Requires a finite floating-point argument. */
    static float requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
        return value;
    }

    /** Requires an ordered finite floating-point interval. */
    static void requireOrdered(float minimum, String minimumName, float maximum, String maximumName) {
        requireFinite(minimum, minimumName);
        requireFinite(maximum, maximumName);
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    minimumName + " must not exceed " + maximumName + ": " + minimum + " > " + maximum);
        }
    }

    /** Requires a non-blank string argument. */
    static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }
}
