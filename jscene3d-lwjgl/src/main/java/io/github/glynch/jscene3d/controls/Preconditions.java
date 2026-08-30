/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

/** Reusable precondition checks for control arguments. */
final class Preconditions {
    /** Prevents instantiation of this validation utility class. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /** Requires a finite non-negative value. */
    static float requireNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and non-negative: " + value);
        }
        return value;
    }

    /** Requires a finite positive value. */
    static float requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + value);
        }
        return value;
    }

    /** Requires a finite value. */
    static float requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
        return value;
    }

    /** Requires a finite value in an inclusive interval. */
    static float requireInRange(float value, String name, float minimum, float maximum) {
        float validValue = requireFinite(value, name);
        if (validValue < minimum || validValue > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between " + minimum + " and " + maximum + ": " + validValue);
        }
        return validValue;
    }

    /** Requires the first value not to exceed the second value. */
    static void requireOrdered(float minimum, String minimumName, float maximum, String maximumName) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    minimumName + " must not exceed " + maximumName + ": " + minimum + " > " + maximum);
        }
    }

    /** Requires a finite value greater than the supplied exclusive lower bound. */
    static float requireGreaterThan(float value, String name, float lowerBound) {
        float validValue = requireFinite(value, name);
        if (validValue <= lowerBound) {
            throw new IllegalArgumentException(name + " must be greater than " + lowerBound + ": " + validValue);
        }
        return validValue;
    }

    /** Requires an interval span to be less than the supplied exclusive maximum. */
    static void requireSpanLessThan(
            float minimum, String minimumName, float maximum, String maximumName, float exclusiveMaximumSpan) {
        if ((double) maximum - minimum >= exclusiveMaximumSpan) {
            throw new IllegalArgumentException(
                    maximumName + " - " + minimumName + " must be less than " + exclusiveMaximumSpan);
        }
    }
}
