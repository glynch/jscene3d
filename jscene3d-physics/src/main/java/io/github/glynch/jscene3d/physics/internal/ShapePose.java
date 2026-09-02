/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Validated shape and world transform used by the internal query engine. */
public final class ShapePose {
    private static final float MINIMUM_QUATERNION_LENGTH_SQUARED = 1.0E-12F;

    private final CollisionShape shape;
    private final Vector3f position;
    private final Quaternionf orientation;

    /**
     * Creates a validated pose and copies its mutable inputs.
     *
     * @param shape immutable collision shape
     * @param position finite world-space position
     * @param orientation finite, non-zero world-space orientation
     */
    public ShapePose(CollisionShape shape, Vector3fc position, Quaternionfc orientation) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.position = requireFinite(position, "position");
        this.orientation = normalized(orientation);
    }

    /**
     * Returns the immutable collision shape.
     *
     * @return collision shape
     */
    public CollisionShape shape() {
        return shape;
    }

    /**
     * Copies the position into the destination.
     *
     * @param destination vector to receive the position
     * @return the supplied destination
     */
    public Vector3f position(Vector3f destination) {
        return destination.set(position);
    }

    /**
     * Copies the orientation into the destination.
     *
     * @param destination quaternion to receive the orientation
     * @return the supplied destination
     */
    public Quaternionf orientation(Quaternionf destination) {
        return destination.set(orientation);
    }

    Vector3f position() {
        return position;
    }

    Quaternionf orientation() {
        return orientation;
    }

    private static Vector3f requireFinite(Vector3fc value, String name) {
        Objects.requireNonNull(value, name);
        if (!value.isFinite()) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return new Vector3f(value);
    }

    private static Quaternionf normalized(Quaternionfc value) {
        Objects.requireNonNull(value, "orientation");
        float lengthSquared = value.lengthSquared();
        if (!Float.isFinite(lengthSquared) || lengthSquared < MINIMUM_QUATERNION_LENGTH_SQUARED) {
            throw new IllegalArgumentException("orientation must be finite and non-zero");
        }
        return new Quaternionf(value).normalize();
    }
}
