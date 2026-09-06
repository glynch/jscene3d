/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/** Immutable-use sphere collision geometry centered at its local origin. */
public final class SphereCollisionShape3dResource implements CollisionShape3dResource {
    private final float radius;
    private boolean closed;

    /**
     * Creates one validated sphere resource.
     *
     * @param radius positive finite radius
     */
    public SphereCollisionShape3dResource(float radius) {
        this.radius = CollisionPreconditions.requirePositive(radius, "radius");
    }

    /**
     * Returns the sphere radius.
     *
     * @return positive radius
     */
    public float radius() {
        requireOpen();
        return radius;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }

    /** Rejects geometry access after lease-owned cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("sphere collision shape resource is closed");
        }
    }
}
