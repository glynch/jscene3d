/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** One solid contact encountered while resolving an explicit kinematic move. */
public final class KinematicContact {
    private final Collider collider;
    private final Vector3f point;
    private final Vector3f normal;

    /**
     * Creates an immutable contact by copying its vectors.
     *
     * @param collider solid collider that constrained movement
     * @param point approximate world-space contact point
     * @param normal world-space contact normal
     */
    public KinematicContact(Collider collider, Vector3fc point, Vector3fc normal) {
        this.collider = Objects.requireNonNull(collider, "collider");
        this.point = new Vector3f(point);
        this.normal = new Vector3f(normal);
    }

    /**
     * Returns the solid collider that constrained the move.
     *
     * @return constraining collider
     */
    public Collider collider() {
        return collider;
    }

    /**
     * Copies the approximate world-space contact point into {@code destination}.
     *
     * @param destination vector to receive the point
     * @return the supplied destination
     */
    public Vector3f point(Vector3f destination) {
        return destination.set(point);
    }

    /**
     * Copies the contact normal that prevents motion into the solid.
     *
     * @param destination vector to receive the normal
     * @return the supplied destination
     */
    public Vector3f normal(Vector3f destination) {
        return destination.set(normal);
    }
}
