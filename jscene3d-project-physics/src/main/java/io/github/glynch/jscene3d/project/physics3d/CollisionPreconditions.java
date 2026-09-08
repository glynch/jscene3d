/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Shared public-boundary validation for the project-physics artifact. */
final class CollisionPreconditions {
    /** Prevents construction of this stateless validation component. */
    private CollisionPreconditions() {
        throw new AssertionError("CollisionPreconditions cannot be instantiated");
    }

    /** Requires a positive finite scalar. */
    static float requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be positive and finite");
        }
        return value;
    }

    /** Requires a non-negative finite scalar. */
    static float requireNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative and finite");
        }
        return value;
    }

    /** Copies one finite vector. */
    static Vector3f requireFinite(Vector3fc value, String name) {
        Vector3fc valid = Objects.requireNonNull(value, name);
        if (!valid.isFinite()) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return new Vector3f(valid);
    }

    /** Copies and normalizes one finite non-zero orientation. */
    static Quaternionf requireOrientation(Quaternionfc value, String name) {
        Quaternionfc valid = Objects.requireNonNull(value, name);
        float lengthSquared = valid.lengthSquared();
        if (!valid.isFinite() || !Float.isFinite(lengthSquared) || lengthSquared == 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and non-zero");
        }
        return new Quaternionf(valid).normalize();
    }
}
