/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.math;

import io.github.glynch.jscene3d.internal.Preconditions;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable spherical three-dimensional bounds. */
public final class BoundingSphere {
    private final Vector3f center;
    private final float radius;

    /**
     * Creates spherical bounds.
     *
     * @param centerX finite center X coordinate
     * @param centerY finite center Y coordinate
     * @param centerZ finite center Z coordinate
     * @param radius finite non-negative radius
     * @throws IllegalArgumentException if any value is not finite or {@code radius} is negative
     */
    public BoundingSphere(float centerX, float centerY, float centerZ, float radius) {
        center = new Vector3f(
                Preconditions.requireFinite(centerX, "centerX"),
                Preconditions.requireFinite(centerY, "centerY"),
                Preconditions.requireFinite(centerZ, "centerZ"));
        float validRadius = Preconditions.requireNonNegative(radius, "radius");
        this.radius = validRadius == 0.0f ? 0.0f : validRadius;
    }

    /**
     * Returns the stable read-only center.
     *
     * @return the center
     */
    public Vector3fc center() {
        return center;
    }

    /**
     * Returns the radius.
     *
     * @return the non-negative radius
     */
    public float radius() {
        return radius;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof BoundingSphere sphere && center.equals(sphere.center) && radius == sphere.radius);
    }

    @Override
    public int hashCode() {
        return 31 * center.hashCode() + Float.hashCode(radius);
    }

    @Override
    public String toString() {
        return "BoundingSphere[center=" + center + ", radius=" + radius + ']';
    }
}
