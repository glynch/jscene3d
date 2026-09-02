/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.queries;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** The first collider reached while translating a shape. */
public final class SweepHit {
    private final Collider collider;
    private final float fraction;
    private final float distance;
    private final Vector3f point;
    private final Vector3f normal;

    /**
     * Creates an immutable sweep result.
     *
     * @param collider collider reached by the query shape
     * @param fraction travel fraction from zero to one
     * @param distance world-space travel distance
     * @param point approximate world-space contact point
     * @param normal direction that prevents travel into the collider
     */
    public SweepHit(Collider collider, float fraction, float distance, Vector3fc point, Vector3fc normal) {
        this.collider = Objects.requireNonNull(collider, "collider");
        this.fraction = fraction;
        this.distance = distance;
        this.point = new Vector3f(point);
        this.normal = new Vector3f(normal);
    }

    /**
     * Returns the collider that was reached.
     *
     * @return reached collider
     */
    public Collider collider() {
        return collider;
    }

    /**
     * Returns travel as a fraction from zero to one.
     *
     * @return normalized travel fraction
     */
    public float fraction() {
        return fraction;
    }

    /**
     * Returns world-space travel distance.
     *
     * @return non-negative travel distance
     */
    public float distance() {
        return distance;
    }

    /**
     * Copies the world-space contact point into the destination.
     *
     * @param destination vector to receive the point
     * @return the supplied destination
     */
    public Vector3f point(Vector3f destination) {
        return destination.set(point);
    }

    /**
     * Copies the direction that prevents movement into the collider.
     *
     * @param destination vector to receive the normal
     * @return the supplied destination
     */
    public Vector3f normal(Vector3f destination) {
        return destination.set(normal);
    }
}
