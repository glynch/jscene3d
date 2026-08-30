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
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    static float requireFinite(float value, String parameterName) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(parameterName + " must be finite: " + value);
        }
        return value;
    }

    static float requirePositive(float value, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue <= 0.0f) {
            throw new IllegalArgumentException(parameterName + " must be positive: " + finiteValue);
        }
        return finiteValue;
    }

    static float requireNonNegative(float value, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue < 0.0f) {
            throw new IllegalArgumentException(parameterName + " must not be negative: " + finiteValue);
        }
        return finiteValue;
    }

    static void requireLessThan(float lowerValue, String lowerName, float upperValue, String upperName) {
        requireFinite(lowerValue, lowerName);
        requireFinite(upperValue, upperName);
        if (lowerValue >= upperValue) {
            throw new IllegalArgumentException(
                    lowerName + " must be less than " + upperName + ": " + lowerValue + " >= " + upperValue);
        }
    }

    static float requireInRange(float value, float minimum, float maximum, String parameterName) {
        float finiteValue = requireFinite(value, parameterName);
        if (finiteValue < minimum || finiteValue > maximum) {
            throw new IllegalArgumentException(
                    parameterName + " must be between " + minimum + " and " + maximum + ": " + finiteValue);
        }
        return finiteValue;
    }

    static int requireInRange(int value, int minimum, int maximum, String parameterName) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    parameterName + " must be between " + minimum + " and " + maximum + ": " + value);
        }
        return value;
    }

    static Vector3fc requireFinite(Vector3fc value, String parameterName) {
        Vector3fc validValue = Objects.requireNonNull(value, parameterName);
        requireFinite(validValue.x(), parameterName + ".x");
        requireFinite(validValue.y(), parameterName + ".y");
        requireFinite(validValue.z(), parameterName + ".z");
        return validValue;
    }

    static Quaternionfc requireFinite(Quaternionfc value, String parameterName) {
        Quaternionfc validValue = Objects.requireNonNull(value, parameterName);
        requireFinite(validValue.x(), parameterName + ".x");
        requireFinite(validValue.y(), parameterName + ".y");
        requireFinite(validValue.z(), parameterName + ".z");
        requireFinite(validValue.w(), parameterName + ".w");
        return validValue;
    }
}
