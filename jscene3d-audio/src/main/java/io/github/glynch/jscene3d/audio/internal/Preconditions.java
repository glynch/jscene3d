/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.internal;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Shared validation for public audio values. */
public final class Preconditions {
    /** Prevents instantiation of this validation container. */
    private Preconditions() {
        throw new AssertionError("Preconditions cannot be instantiated");
    }

    /**
     * Returns a finite value in the inclusive unit interval.
     *
     * @param value value to validate
     * @param name argument name used in failures
     * @return validated value
     */
    public static float requireUnitInterval(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be finite and between zero and one: " + value);
        }
        return value;
    }

    /**
     * Returns a non-negative finite value.
     *
     * @param value value to validate
     * @param name argument name used in failures
     * @return validated value
     */
    public static float requireNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative and finite: " + value);
        }
        return value;
    }

    /**
     * Returns a positive finite value.
     *
     * @param value value to validate
     * @param name argument name used in failures
     * @return validated value
     */
    public static float requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be positive and finite: " + value);
        }
        return value;
    }

    /**
     * Returns a finite defensive copy of a vector.
     *
     * @param value vector to validate
     * @param name argument name used in failures
     * @return finite defensive copy
     */
    public static Vector3f requireFinite(Vector3fc value, String name) {
        Vector3fc validValue = Objects.requireNonNull(value, name);
        if (!Float.isFinite(validValue.x()) || !Float.isFinite(validValue.y()) || !Float.isFinite(validValue.z())) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return new Vector3f(validValue);
    }

    /**
     * Returns a finite, normalized defensive copy of a direction vector.
     *
     * @param value vector to validate
     * @param name argument name used in failures
     * @return finite normalized defensive copy
     */
    public static Vector3f requireDirection(Vector3fc value, String name) {
        Vector3f validValue = requireFinite(value, name);
        if (validValue.lengthSquared() == 0.0F) {
            throw new IllegalArgumentException(name + " must not be zero");
        }
        return validValue.normalize();
    }
}
