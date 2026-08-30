/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Objects;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** Reusable precondition checks for core arguments. */
final class Preconditions {
    /** Prevents instantiation of this validation utility class. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /** Requires a finite floating-point value. */
    static float requireFinite(float value, String parameterName) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(parameterName + " must be finite: " + value);
        }
        return value;
    }

    /** Requires a finite positive floating-point value. */
    static float requirePositive(float value, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue <= 0.0f) {
            throw new IllegalArgumentException(parameterName + " must be positive: " + finiteValue);
        }
        return finiteValue;
    }

    /** Requires a finite non-negative floating-point value. */
    static float requireNonNegative(float value, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue < 0.0f) {
            throw new IllegalArgumentException(parameterName + " must not be negative: " + finiteValue);
        }
        return finiteValue;
    }

    /** Requires two finite values in strictly increasing order. */
    static void requireLessThan(float lowerValue, String lowerName, float upperValue, String upperName) {
        requireFinite(lowerValue, lowerName);
        requireFinite(upperValue, upperName);
        if (lowerValue >= upperValue) {
            throw new IllegalArgumentException(
                    lowerName + " must be less than " + upperName + ": " + lowerValue + " >= " + upperValue);
        }
    }

    /** Requires a finite floating-point value in an inclusive interval. */
    static float requireInRange(float value, float minimum, float maximum, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue < minimum || finiteValue > maximum) {
            throw new IllegalArgumentException(
                    parameterName + " must be between " + minimum + " and " + maximum + ": " + finiteValue);
        }
        return finiteValue;
    }

    /** Requires an integer value in an inclusive interval. */
    static int requireInRange(int value, int minimum, int maximum, String parameterName) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    parameterName + " must be between " + minimum + " and " + maximum + ": " + value);
        }
        return value;
    }

    /** Requires a positive integer value. */
    static int requirePositive(int value, String parameterName) {
        if (value <= 0) {
            throw new IllegalArgumentException(parameterName + " must be positive: " + value);
        }
        return value;
    }

    /** Requires a non-negative integer value. */
    static int requireNonNegative(int value, String parameterName) {
        if (value < 0) {
            throw new IllegalArgumentException(parameterName + " must not be negative: " + value);
        }
        return value;
    }

    /** Requires a non-null, non-empty string. */
    static String requireNonEmpty(String value, String parameterName) {
        String validValue = Objects.requireNonNull(value, parameterName);
        if (validValue.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " must not be empty");
        }
        return validValue;
    }

    /** Returns a checked Java-array length for an item count and item size. */
    static int requireArrayLength(long itemCount, int itemSize, String parameterName) {
        if (itemCount < 0L || itemSize <= 0 || itemCount > Integer.MAX_VALUE / itemSize) {
            throw new IllegalArgumentException(parameterName + " data exceeds Java array limits");
        }
        return (int) itemCount * itemSize;
    }

    /** Requires a non-null vector with finite components. */
    static Vector3fc requireFinite(Vector3fc value, String parameterName) {
        Vector3fc validValue = Objects.requireNonNull(value, parameterName);
        requireFinite(validValue.x(), parameterName + ".x");
        requireFinite(validValue.y(), parameterName + ".y");
        requireFinite(validValue.z(), parameterName + ".z");
        return validValue;
    }

    /** Requires a non-null quaternion with finite components. */
    static Quaternionfc requireFinite(Quaternionfc value, String parameterName) {
        Quaternionfc validValue = Objects.requireNonNull(value, parameterName);
        requireFinite(validValue.x(), parameterName + ".x");
        requireFinite(validValue.y(), parameterName + ".y");
        requireFinite(validValue.z(), parameterName + ".z");
        requireFinite(validValue.w(), parameterName + ".w");
        return validValue;
    }
}
