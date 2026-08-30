/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Creates indexed rectangular geometry in the XY plane facing positive Z. */
public final class PlaneGeometry {
    /** Prevents instantiation of this geometry factory. */
    private PlaneGeometry() {
        throw new AssertionError("PlaneGeometry cannot be instantiated");
    }

    /**
     * Creates a centered rectangular plane with four vertices and two triangles.
     *
     * @param width finite positive X-axis extent
     * @param height finite positive Y-axis extent
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a dimension is not finite and positive
     */
    public static BufferGeometry create(float width, float height) {
        float halfWidth = Preconditions.requirePositive(width, "width") * 0.5f;
        float halfHeight = Preconditions.requirePositive(height, "height") * 0.5f;
        float[] positions = {
            -halfWidth,
            -halfHeight,
            0.0f,
            halfWidth,
            -halfHeight,
            0.0f,
            halfWidth,
            halfHeight,
            0.0f,
            -halfWidth,
            halfHeight,
            0.0f
        };
        float[] normals = {
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f
        };
        float[] textureCoordinates = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };
        int[] indices = {0, 1, 2, 0, 2, 3};
        return BufferGeometryFactorySupport.create(positions, normals, textureCoordinates, indices);
    }
}
