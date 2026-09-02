/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.queries;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** The nearest surface encountered by a ray. */
public final class RaycastHit {
    private final Collider collider;
    private final float distance;
    private final Vector3f point;
    private final Vector3f normal;

    /**
     * Creates an immutable raycast result.
     *
     * @param collider collider reached by the ray
     * @param distance distance from the ray origin
     * @param point world-space surface point
     * @param normal outward world-space surface normal
     */
    public RaycastHit(Collider collider, float distance, Vector3fc point, Vector3fc normal) {
        this.collider = Objects.requireNonNull(collider, "collider");
        this.distance = distance;
        this.point = new Vector3f(point);
        this.normal = new Vector3f(normal);
    }

    /**
     * Returns the collider that was hit.
     *
     * @return hit collider
     */
    public Collider collider() {
        return collider;
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
        return destination.set(point);
    }

    /**
     * Copies the outward world-space surface normal into the destination.
     *
     * @param destination vector to receive the normal
     * @return the supplied destination
     */
    public Vector3f normal(Vector3f destination) {
        return destination.set(normal);
    }
}
