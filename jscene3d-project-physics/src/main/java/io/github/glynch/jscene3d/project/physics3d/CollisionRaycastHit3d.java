/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Nearest project-level collision surface encountered by a ray. */
public final class CollisionRaycastHit3d {
    private final CollisionObject3d object;
    private final CollisionShape3d shape;
    private final float distance;
    private final Vector3f point;
    private final Vector3f normal;

    /**
     * Creates an immutable project-level raycast result.
     *
     * @param object body or sensor owning the hit shape
     * @param shape precise shape reached by the ray
     * @param distance distance from the ray origin
     * @param point world-space surface point
     * @param normal outward world-space surface normal
     */
    public CollisionRaycastHit3d(
            CollisionObject3d object, CollisionShape3d shape, float distance, Vector3fc point, Vector3fc normal) {
        this.object = Objects.requireNonNull(object, "object");
        this.shape = Objects.requireNonNull(shape, "shape");
        this.distance = distance;
        this.point = new Vector3f(Objects.requireNonNull(point, "point"));
        this.normal = new Vector3f(Objects.requireNonNull(normal, "normal"));
    }

    /**
     * Returns the project collision object owning the hit shape.
     *
     * @return owning collision object
     */
    public CollisionObject3d object() {
        return object;
    }

    /**
     * Returns the precise project collision shape reached by the ray.
     *
     * @return hit collision shape
     */
    public CollisionShape3d shape() {
        return shape;
    }

    /**
     * Returns distance from the ray origin along its normalized direction.
     *
     * @return non-negative world-space distance
     */
    public float distance() {
        return distance;
    }

    /**
     * Copies the world-space hit point into the destination.
     *
     * @param destination vector to receive the point
     * @return the supplied destination
     */
    public Vector3f point(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(point);
    }

    /**
     * Copies the outward world-space surface normal into the destination.
     *
     * @param destination vector to receive the normal
     * @return the supplied destination
     */
    public Vector3f normal(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(normal);
    }
}
