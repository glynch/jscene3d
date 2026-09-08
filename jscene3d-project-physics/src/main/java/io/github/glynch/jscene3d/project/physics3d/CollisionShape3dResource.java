/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/**
 * Shared immutable-use collision geometry retained through a world-owned runtime-resource lease.
 *
 * <p>Implementations are backend-independent. Closing is idempotent and terminal; callers normally let the composed
 * world release the resource after its final component lease.
 */
public sealed interface CollisionShape3dResource extends AutoCloseable
        permits BoxCollisionShape3dResource,
                CapsuleCollisionShape3dResource,
                SphereCollisionShape3dResource,
                TriangleMeshCollisionShape3dResource {
    /**
     * Returns whether the resource has been released.
     *
     * @return {@code true} after closure
     */
    boolean isClosed();

    /** Releases this resource exactly once. */
    @Override
    void close();
}
