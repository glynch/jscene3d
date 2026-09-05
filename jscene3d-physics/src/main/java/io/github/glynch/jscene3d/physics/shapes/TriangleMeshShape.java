/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.shapes;

import io.github.glynch.jscene3d.physics.internal.Preconditions;
import java.util.Arrays;
import java.util.Objects;
import org.joml.Vector3f;

/**
 * Immutable indexed triangle collision mesh for static level geometry.
 *
 * <p>Positions contain consecutive XYZ coordinates. Indices contain consecutive triples and may
 * reuse vertices. Degenerate triangles are rejected because they do not define a stable collision
 * plane. Triangle meshes can be attached only to static bodies and cannot be used as overlap or
 * sweep query shapes.
 */
public final class TriangleMeshShape implements CollisionShape {
    private static final float MINIMUM_NORMAL_LENGTH_SQUARED = 1.0E-12F;

    private final float[] positions;
    private final int[] indices;

    /**
     * Creates an immutable indexed triangle mesh by copying both arrays.
     *
     * @param positions consecutive finite XYZ coordinates
     * @param indices consecutive vertex-index triples
     */
    public TriangleMeshShape(float[] positions, int[] indices) {
        this.positions = requirePositions(positions);
        this.indices = requireIndices(indices, this.positions.length / 3);
        requireNonDegenerateTriangles();
    }

    /**
     * Returns the number of vertices.
     *
     * @return vertex count
     */
    public int vertexCount() {
        return positions.length / 3;
    }

    /**
     * Returns the number of indexed triangles.
     *
     * @return triangle count
     */
    public int triangleCount() {
        return indices.length / 3;
    }

    /**
     * Copies one local-space vertex into the supplied destination.
     *
     * @param vertexIndex zero-based vertex index
     * @param destination destination vector
     * @return supplied destination
     */
    public Vector3f vertex(int vertexIndex, Vector3f destination) {
        Objects.checkIndex(vertexIndex, vertexCount());
        Objects.requireNonNull(destination, "destination");
        int offset = vertexIndex * 3;
        return destination.set(positions[offset], positions[offset + 1], positions[offset + 2]);
    }

    /**
     * Returns one vertex index from the flattened triangle-index sequence.
     *
     * @param indexOffset zero-based offset in the flattened index sequence
     * @return referenced vertex index
     */
    public int index(int indexOffset) {
        return indices[Objects.checkIndex(indexOffset, indices.length)];
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof TriangleMeshShape mesh
                && Arrays.equals(positions, mesh.positions)
                && Arrays.equals(indices, mesh.indices);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(positions) + Arrays.hashCode(indices);
    }

    @Override
    public String toString() {
        return "TriangleMeshShape[positions=" + Arrays.toString(positions) + ", indices=" + Arrays.toString(indices)
                + ']';
    }

    /** Copies and validates the flattened vertex positions. */
    private static float[] requirePositions(float[] values) {
        Objects.requireNonNull(values, "positions");
        if (values.length < 9 || values.length % 3 != 0) {
            throw new IllegalArgumentException("positions must contain at least three complete XYZ vertices");
        }
        float[] copy = values.clone();
        for (int index = 0; index < copy.length; index++) {
            Preconditions.requireFinite(copy[index], "positions[" + index + ']');
        }
        return copy;
    }

    /** Copies and validates the flattened triangle indices. */
    private static int[] requireIndices(int[] values, int vertexCount) {
        Objects.requireNonNull(values, "indices");
        if (values.length < 3 || values.length % 3 != 0) {
            throw new IllegalArgumentException("indices must contain at least one complete triangle");
        }
        int[] copy = values.clone();
        for (int offset = 0; offset < copy.length; offset++) {
            if (copy[offset] < 0 || copy[offset] >= vertexCount) {
                throw new IllegalArgumentException(
                        "indices[" + offset + "] is outside the vertex range: " + copy[offset]);
            }
        }
        return copy;
    }

    /** Rejects indexed triangles whose vertices are coincident or collinear. */
    private void requireNonDegenerateTriangles() {
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        for (int triangle = 0; triangle < triangleCount(); triangle++) {
            int offset = triangle * 3;
            vertex(indices[offset], first);
            vertex(indices[offset + 1], second);
            vertex(indices[offset + 2], third);
            float normalLengthSquared = second.sub(first, new Vector3f())
                    .cross(third.sub(first, new Vector3f()))
                    .lengthSquared();
            if (normalLengthSquared < MINIMUM_NORMAL_LENGTH_SQUARED) {
                throw new IllegalArgumentException("triangle " + triangle + " is degenerate");
            }
        }
    }
}
