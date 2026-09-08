/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.geometry;

import java.util.Arrays;
import java.util.Objects;

/** Immutable indexed triangle data independent of JScene3D and native rendering. */
public final class DoomMeshData {
    private final float[] positions;
    private final float[] normals;
    private final float[] textureCoordinates;
    private final int[] indices;

    /**
     * Creates triangle data by defensively copying its flat component arrays.
     *
     * @param positions flat XYZ vertex positions
     * @param normals flat XYZ vertex normals
     * @param textureCoordinates flat UV texture coordinates
     * @param indices triangle vertex indices
     */
    public DoomMeshData(float[] positions, float[] normals, float[] textureCoordinates, int[] indices) {
        this.positions = requireComponents(positions, 3, "positions");
        this.normals = requireComponents(normals, 3, "normals");
        this.textureCoordinates = requireComponents(textureCoordinates, 2, "textureCoordinates");
        this.indices = Objects.requireNonNull(indices, "indices").clone();
        int vertexCount = this.positions.length / 3;
        if (this.normals.length / 3 != vertexCount || this.textureCoordinates.length / 2 != vertexCount) {
            throw new IllegalArgumentException("position, normal, and texture-coordinate counts must match");
        }
        if (this.indices.length % 3 != 0) {
            throw new IllegalArgumentException("indices must form complete triangles");
        }
        for (int index : this.indices) {
            if (index < 0 || index >= vertexCount) {
                throw new IllegalArgumentException("index is outside the vertex range: " + index);
            }
        }
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
     * Returns the number of triangles.
     *
     * @return triangle count
     */
    public int triangleCount() {
        return indices.length / 3;
    }

    /**
     * Returns a defensive XYZ copy for one vertex.
     *
     * @param vertex zero-based vertex index
     * @return three XYZ components
     */
    public float[] position(int vertex) {
        return components(positions, Objects.checkIndex(vertex, vertexCount()), 3);
    }

    /**
     * Returns a defensive XYZ copy for one vertex normal.
     *
     * @param vertex zero-based vertex index
     * @return three normal components
     */
    public float[] normal(int vertex) {
        return components(normals, Objects.checkIndex(vertex, vertexCount()), 3);
    }

    /**
     * Returns a defensive UV copy for one vertex.
     *
     * @param vertex zero-based vertex index
     * @return two UV components
     */
    public float[] textureCoordinate(int vertex) {
        return components(textureCoordinates, Objects.checkIndex(vertex, vertexCount()), 2);
    }

    /**
     * Returns a defensive copy of all flat XYZ positions.
     *
     * @return copied position components
     */
    public float[] positions() {
        return positions.clone();
    }

    /**
     * Returns a defensive copy of all flat XYZ normals.
     *
     * @return copied normal components
     */
    public float[] normals() {
        return normals.clone();
    }

    /**
     * Returns a defensive copy of all flat UV coordinates.
     *
     * @return copied texture-coordinate components
     */
    public float[] textureCoordinates() {
        return textureCoordinates.clone();
    }

    /**
     * Returns a defensive copy of all triangle indices.
     *
     * @return copied indices
     */
    public int[] indices() {
        return indices.clone();
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) {
            return true;
        }
        if (!(candidate instanceof DoomMeshData other)) {
            return false;
        }
        return Arrays.equals(positions, other.positions)
                && Arrays.equals(normals, other.normals)
                && Arrays.equals(textureCoordinates, other.textureCoordinates)
                && Arrays.equals(indices, other.indices);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(positions);
        result = 31 * result + Arrays.hashCode(normals);
        result = 31 * result + Arrays.hashCode(textureCoordinates);
        return 31 * result + Arrays.hashCode(indices);
    }

    @Override
    public String toString() {
        return "DoomMeshData[vertexCount=" + vertexCount() + ", triangleCount=" + triangleCount() + "]";
    }

    private static float[] requireComponents(float[] values, int componentCount, String name) {
        float[] copy = Objects.requireNonNull(values, name).clone();
        if (copy.length % componentCount != 0) {
            throw new IllegalArgumentException(name + " must contain complete components");
        }
        for (float value : copy) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(name + " must contain finite values");
            }
        }
        return copy;
    }

    private static float[] components(float[] values, int item, int componentCount) {
        int offset = item * componentCount;
        return Arrays.copyOfRange(values, offset, offset + componentCount);
    }
}
