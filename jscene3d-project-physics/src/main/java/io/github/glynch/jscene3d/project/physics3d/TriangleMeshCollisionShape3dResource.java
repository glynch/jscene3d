/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import java.util.Objects;
import org.joml.Vector3f;

/** Immutable-use indexed triangle collision geometry restricted to static bodies. */
public final class TriangleMeshCollisionShape3dResource implements CollisionShape3dResource {
    private final TriangleMeshShape shape;
    private boolean closed;

    /**
     * Creates one collision mesh after defensively copying and validating its arrays.
     *
     * @param positions consecutive finite XYZ coordinates
     * @param indices consecutive vertex-index triples
     */
    public TriangleMeshCollisionShape3dResource(float[] positions, int[] indices) {
        shape = new TriangleMeshShape(
                Objects.requireNonNull(positions, "positions"), Objects.requireNonNull(indices, "indices"));
    }

    /**
     * Returns the number of vertices.
     *
     * @return vertex count
     */
    public int vertexCount() {
        requireOpen();
        return shape.vertexCount();
    }

    /**
     * Returns the number of indexed triangles.
     *
     * @return triangle count
     */
    public int triangleCount() {
        requireOpen();
        return shape.triangleCount();
    }

    /**
     * Copies one local-space vertex into the supplied destination.
     *
     * @param vertexIndex zero-based vertex index
     * @param destination destination vector
     * @return supplied destination
     */
    public Vector3f vertex(int vertexIndex, Vector3f destination) {
        requireOpen();
        return shape.vertex(vertexIndex, destination);
    }

    /**
     * Returns one vertex index from the flattened triangle-index sequence.
     *
     * @param indexOffset zero-based offset in the flattened index sequence
     * @return referenced vertex index
     */
    public int index(int indexOffset) {
        requireOpen();
        return shape.index(indexOffset);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }

    /** Returns the immutable backend shape while this resource remains open. */
    TriangleMeshShape shape() {
        requireOpen();
        return shape;
    }

    /** Rejects collision-geometry access after lease-owned cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("triangle mesh collision shape resource is closed");
        }
    }
}
