/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import java.util.Objects;

/**
 * Shared immutable-use runtime mesh resource.
 *
 * <p>{@link #owning(BufferGeometry)} transfers exclusive ownership of the supplied open geometry. The caller must not
 * retain or mutate that geometry afterwards. Runtime components retain this resource through world-owned leases and
 * never close it directly.
 */
public final class Mesh3dResource implements AutoCloseable {
    private final BufferGeometry geometry;

    /** Terminal resource state. */
    private boolean closed;

    /** Takes ownership of one validated geometry. */
    private Mesh3dResource(BufferGeometry geometry) {
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        if (geometry.isClosed()) {
            throw new IllegalArgumentException("geometry must be open");
        }
    }

    /**
     * Takes exclusive ownership of an open geometry as an immutable-use runtime resource.
     *
     * @param geometry geometry whose ownership transfers
     * @return new resource
     * @throws NullPointerException if {@code geometry} is {@code null}
     * @throws IllegalArgumentException if {@code geometry} is closed
     */
    public static Mesh3dResource owning(BufferGeometry geometry) {
        return new Mesh3dResource(geometry);
    }

    /**
     * Returns whether this resource and its owned geometry are closed.
     *
     * @return {@code true} after terminal closure
     */
    public boolean isClosed() {
        return closed;
    }

    /** Closes the owned geometry exactly once; repeated calls have no effect. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        geometry.close();
    }

    /** Returns the internal geometry while this resource is open. */
    BufferGeometry geometry() {
        requireOpen();
        return geometry;
    }

    /** Rejects internal access after terminal closure. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Mesh3dResource is closed");
        }
    }
}
