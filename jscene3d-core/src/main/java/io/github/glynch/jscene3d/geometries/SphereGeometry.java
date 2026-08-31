/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import io.github.glynch.jscene3d.internal.Preconditions;

/** Creates indexed UV-sphere geometry centered at the origin. */
public final class SphereGeometry {
    private static final int DEFAULT_WIDTH_SEGMENTS = 32;
    private static final int DEFAULT_HEIGHT_SEGMENTS = 16;

    /** Prevents instantiation of this geometry factory. */
    private SphereGeometry() {
        throw new AssertionError("SphereGeometry cannot be instantiated");
    }

    /**
     * Creates a sphere using 32 longitudinal and 16 latitudinal segments.
     *
     * @param radius finite positive radius
     * @return new application-owned geometry
     * @throws IllegalArgumentException if {@code radius} is not finite and positive
     */
    public static BufferGeometry create(float radius) {
        return create(radius, DEFAULT_WIDTH_SEGMENTS, DEFAULT_HEIGHT_SEGMENTS);
    }

    /**
     * Creates a configurable UV sphere.
     *
     * @param radius finite positive radius
     * @param widthSegments longitudinal segments, at least three
     * @param heightSegments latitudinal segments, at least two
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or the requested arrays exceed
     *     Java array limits
     */
    public static BufferGeometry create(float radius, int widthSegments, int heightSegments) {
        float validRadius = Preconditions.requirePositive(radius, "radius");
        int validWidthSegments = Preconditions.requireInRange(widthSegments, 3, Integer.MAX_VALUE, "widthSegments");
        int validHeightSegments = Preconditions.requireInRange(heightSegments, 2, Integer.MAX_VALUE, "heightSegments");
        long vertexCount = (validWidthSegments + 1L) * (validHeightSegments + 1L);
        long triangleCornerGroupCount = validWidthSegments * (validHeightSegments - 1L);
        int positionLength = Preconditions.requireArrayLength(vertexCount, 3, "position and normal");
        int textureCoordinateLength = Preconditions.requireArrayLength(vertexCount, 2, "texture-coordinate");
        int indexCount = Preconditions.requireArrayLength(triangleCornerGroupCount, 6, "index");

        float[] positions = new float[positionLength];
        float[] normals = new float[positionLength];
        float[] textureCoordinates = new float[textureCoordinateLength];
        int positionOffset = 0;
        int textureCoordinateOffset = 0;
        for (int latitude = 0; latitude <= validHeightSegments; latitude++) {
            float v = (float) latitude / validHeightSegments;
            double polarAngle = v * Math.PI;
            float normalY = (float) Math.cos(polarAngle);
            float ringRadius = latitude == 0 || latitude == validHeightSegments ? 0.0f : (float) Math.sin(polarAngle);
            for (int longitude = 0; longitude <= validWidthSegments; longitude++) {
                float u = (float) longitude / validWidthSegments;
                double azimuth = u * Math.PI * 2.0;
                float normalX = ringRadius * (float) Math.cos(azimuth);
                float normalZ = ringRadius * (float) Math.sin(azimuth);
                positions[positionOffset] = normalX * validRadius;
                normals[positionOffset++] = normalX;
                positions[positionOffset] = normalY * validRadius;
                normals[positionOffset++] = normalY;
                positions[positionOffset] = normalZ * validRadius;
                normals[positionOffset++] = normalZ;
                textureCoordinates[textureCoordinateOffset++] = u;
                textureCoordinates[textureCoordinateOffset++] = 1.0f - v;
            }
        }

        int[] indices = new int[indexCount];
        int indexOffset = 0;
        int rowLength = validWidthSegments + 1;
        for (int latitude = 0; latitude < validHeightSegments; latitude++) {
            for (int longitude = 0; longitude < validWidthSegments; longitude++) {
                int topLeft = latitude * rowLength + longitude;
                int topRight = topLeft + 1;
                int bottomLeft = topLeft + rowLength;
                int bottomRight = bottomLeft + 1;
                if (latitude > 0) {
                    indices[indexOffset++] = topLeft;
                    indices[indexOffset++] = topRight;
                    indices[indexOffset++] = bottomLeft;
                }
                if (latitude < validHeightSegments - 1) {
                    indices[indexOffset++] = topRight;
                    indices[indexOffset++] = bottomRight;
                    indices[indexOffset++] = bottomLeft;
                }
            }
        }
        return BufferGeometryFactorySupport.create(positions, normals, textureCoordinates, indices);
    }
}
