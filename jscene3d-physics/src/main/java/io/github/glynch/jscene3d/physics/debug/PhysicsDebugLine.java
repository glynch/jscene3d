/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.debug;

import io.github.glynch.jscene3d.physics.Collider;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** One world-space line segment associated with a collider in a debug snapshot. */
public final class PhysicsDebugLine {
    private final Collider collider;
    private final Vector3f start;
    private final Vector3f end;

    /**
     * Creates an immutable line by copying both endpoints.
     *
     * @param collider collider represented by this segment
     * @param start world-space segment start
     * @param end world-space segment end
     */
    public PhysicsDebugLine(Collider collider, Vector3fc start, Vector3fc end) {
        this.collider = Objects.requireNonNull(collider, "collider");
        this.start = new Vector3f(start);
        this.end = new Vector3f(end);
    }

    /** Returns the collider represented by this line.
     * @return represented collider
     */
    public Collider collider() {
        return collider;
    }

    /** Copies the world-space start into {@code destination}.
     * @param destination vector to receive the start
     * @return the supplied destination
     */
    public Vector3f start(Vector3f destination) {
        return destination.set(start);
    }

    /** Copies the world-space end into {@code destination}.
     * @param destination vector to receive the end
     * @return the supplied destination
     */
    public Vector3f end(Vector3f destination) {
        return destination.set(end);
    }
}
