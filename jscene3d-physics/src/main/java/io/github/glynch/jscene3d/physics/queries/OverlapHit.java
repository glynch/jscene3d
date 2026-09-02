/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.queries;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** A collider overlapping a query shape. */
public final class OverlapHit {
    private final Collider collider;
    private final float penetrationDepth;
    private final Vector3f normal;

    /**
     * Creates an immutable overlap result.
     *
     * @param collider overlapping collider
     * @param penetrationDepth minimum translation distance
     * @param normal direction that moves the query shape out of the collider
     */
    public OverlapHit(Collider collider, float penetrationDepth, Vector3fc normal) {
        this.collider = Objects.requireNonNull(collider, "collider");
        this.penetrationDepth = penetrationDepth;
        this.normal = new Vector3f(normal);
    }

    /**
     * Returns the overlapping collider.
     *
     * @return overlapping collider
     */
    public Collider collider() {
        return collider;
    }

    /**
     * Returns the minimum translation distance along the normal.
     *
     * @return non-negative penetration depth
     */
    public float penetrationDepth() {
        return penetrationDepth;
    }

    /**
     * Copies the direction that moves the query shape out of the collider.
     *
     * @param destination vector to receive the normal
     * @return the supplied destination
     */
    public Vector3f normal(Vector3f destination) {
        return destination.set(normal);
    }
}
