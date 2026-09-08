/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/** Immutable-use capsule collision geometry centered at its local origin and aligned to local Y. */
public final class CapsuleCollisionShape3dResource implements CollisionShape3dResource {
    private final float radius;
    private final float segmentLength;
    private boolean closed;

    /**
     * Creates one validated capsule resource.
     *
     * @param radius positive finite radius of the cylindrical body and hemispherical caps
     * @param segmentLength finite non-negative distance between the cap centres
     */
    public CapsuleCollisionShape3dResource(float radius, float segmentLength) {
        this.radius = CollisionPreconditions.requirePositive(radius, "radius");
        this.segmentLength = CollisionPreconditions.requireNonNegative(segmentLength, "segmentLength");
    }

    /**
     * Returns the capsule radius.
     *
     * @return positive radius
     */
    public float radius() {
        requireOpen();
        return radius;
    }

    /**
     * Returns the length of the cylindrical segment between both cap centres.
     *
     * @return finite non-negative segment length
     */
    public float segmentLength() {
        requireOpen();
        return segmentLength;
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
            throw new IllegalStateException("capsule collision shape resource is closed");
        }
    }
}
