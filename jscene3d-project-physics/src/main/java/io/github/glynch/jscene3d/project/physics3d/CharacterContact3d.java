/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** One project-level solid contact encountered while resolving character movement. */
public final class CharacterContact3d {
    private final CollisionObject3d object;
    private final CollisionShape3d shape;
    private final Vector3f point;
    private final Vector3f normal;

    /**
     * Creates an immutable contact by copying its vectors.
     *
     * @param object collision object that constrained movement
     * @param shape precise member shape that constrained movement
     * @param point approximate world-space contact point
     * @param normal world-space contact normal
     */
    public CharacterContact3d(CollisionObject3d object, CollisionShape3d shape, Vector3fc point, Vector3fc normal) {
        this.object = Objects.requireNonNull(object, "object");
        this.shape = Objects.requireNonNull(shape, "shape");
        this.point = CollisionPreconditions.requireFinite(point, "point");
        this.normal = CollisionPreconditions.requireFinite(normal, "normal");
    }

    /**
     * Returns the collision object that constrained movement.
     *
     * @return constraining collision object
     */
    public CollisionObject3d object() {
        return object;
    }

    /**
     * Returns the precise member shape that constrained movement.
     *
     * @return constraining collision shape
     */
    public CollisionShape3d shape() {
        return shape;
    }

    /**
     * Copies the approximate world-space contact point.
     *
     * @param destination vector to receive the point
     * @return supplied destination
     */
    public Vector3f point(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(point);
    }

    /**
     * Copies the world-space contact normal.
     *
     * @param destination vector to receive the normal
     * @return supplied destination
     */
    public Vector3f normal(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(normal);
    }
}
