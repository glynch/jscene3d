/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/** Immutable-use box collision geometry whose dimensions are full local extents. */
public final class BoxCollisionShape3dResource implements CollisionShape3dResource {
    private final float width;
    private final float height;
    private final float depth;
    private boolean closed;

    /**
     * Creates one validated box resource.
     *
     * @param width positive finite X extent
     * @param height positive finite Y extent
     * @param depth positive finite Z extent
     */
    public BoxCollisionShape3dResource(float width, float height, float depth) {
        this.width = CollisionPreconditions.requirePositive(width, "width");
        this.height = CollisionPreconditions.requirePositive(height, "height");
        this.depth = CollisionPreconditions.requirePositive(depth, "depth");
    }

    /**
     * Returns the full X extent.
     *
     * @return positive full X extent
     */
    public float width() {
        requireOpen();
        return width;
    }

    /**
     * Returns the full Y extent.
     *
     * @return positive full Y extent
     */
    public float height() {
        requireOpen();
        return height;
    }

    /**
     * Returns the full Z extent.
     *
     * @return positive full Z extent
     */
    public float depth() {
        requireOpen();
        return depth;
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
            throw new IllegalStateException("box collision shape resource is closed");
        }
    }
}
