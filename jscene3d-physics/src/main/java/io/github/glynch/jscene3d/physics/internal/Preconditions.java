/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Shared implementation-only physics argument validation. */
public final class Preconditions {
    private static final float MINIMUM_DIRECTION_LENGTH_SQUARED = 1.0E-12F;
    private static final float MINIMUM_ORIENTATION_LENGTH_SQUARED = 1.0E-12F;

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

    /**
     * Requires and copies a finite three-dimensional vector.
     *
     * @param value vector to validate
     * @param parameterName parameter name used in diagnostics
     * @return validated vector copy
     */
    public static Vector3f requireFinite(Vector3fc value, String parameterName) {
        Objects.requireNonNull(value, parameterName);
        if (!value.isFinite()) {
            throw new IllegalArgumentException(parameterName + " must be finite");
        }
        return new Vector3f(value);
    }

    /**
     * Requires and normalizes a finite, non-zero direction.
     *
     * @param value direction to validate
     * @param parameterName parameter name used in diagnostics
     * @return normalized direction copy
     */
    public static Vector3f requireDirection(Vector3fc value, String parameterName) {
        Vector3f direction = requireFinite(value, parameterName);
        if (direction.lengthSquared() < MINIMUM_DIRECTION_LENGTH_SQUARED) {
            throw new IllegalArgumentException(parameterName + " must be non-zero");
        }
        return direction.normalize();
    }

    /**
     * Requires and normalizes a finite, non-zero orientation.
     *
     * @param value orientation to validate
     * @param parameterName parameter name used in diagnostics
     * @return normalized orientation copy
     */
    public static Quaternionf requireOrientation(Quaternionfc value, String parameterName) {
        Objects.requireNonNull(value, parameterName);
        float lengthSquared = value.lengthSquared();
        if (!Float.isFinite(lengthSquared) || lengthSquared < MINIMUM_ORIENTATION_LENGTH_SQUARED) {
            throw new IllegalArgumentException(parameterName + " must be finite and non-zero");
        }
        return new Quaternionf(value).normalize();
    }
}
