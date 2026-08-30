/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Creates indexed box geometry with independent face normals and texture coordinates. */
public final class BoxGeometry {
    private BoxGeometry() {
        throw new AssertionError("BoxGeometry cannot be instantiated");
    }

    /**
     * Creates a centered box with 24 vertices and 12 triangles.
     *
     * @param width finite positive X-axis extent
     * @param height finite positive Y-axis extent
     * @param depth finite positive Z-axis extent
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a dimension is not finite and positive
     */
    public static BufferGeometry create(float width, float height, float depth) {
        float x = Preconditions.requirePositive(width, "width") * 0.5f;
        float y = Preconditions.requirePositive(height, "height") * 0.5f;
        float z = Preconditions.requirePositive(depth, "depth") * 0.5f;
        float[] positions = {
            x, -y, z, x, -y, -z, x, y, -z, x, y, z, -x, -y, -z, -x, -y, z, -x, y, z, -x, y, -z, -x, y, z, x, y, z, x, y,
            -z, -x, y, -z, -x, -y, -z, x, -y, -z, x, -y, z, -x, -y, z, -x, -y, z, x, -y, z, x, y, z, -x, y, z, x, -y,
            -z, -x, -y, -z, -x, y, -z, x, y, -z
        };
        float[] normals = {
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, -1.0f
        };
        float[] textureCoordinates = {
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f
        };
        int[] indices = {
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
        };
        return BufferGeometryFactorySupport.create(positions, normals, textureCoordinates, indices);
    }
}
