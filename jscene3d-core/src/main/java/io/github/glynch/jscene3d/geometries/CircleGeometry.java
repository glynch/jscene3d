/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.internal.Preconditions;

/** Creates indexed circular sectors in the XY plane facing positive Z. */
public final class CircleGeometry {
    private static final int DEFAULT_SEGMENTS = 32;

    /** Prevents instantiation of this geometry factory. */
    private CircleGeometry() {
        throw new AssertionError("CircleGeometry cannot be instantiated");
    }

    /**
     * Creates a complete circle using 32 angular segments.
     *
     * @param radius finite positive radius
     * @return new application-owned geometry
     * @throws IllegalArgumentException if {@code radius} is not finite and positive
     */
    public static BufferGeometry create(float radius) {
        return create(radius, DEFAULT_SEGMENTS);
    }

    /**
     * Creates a complete circle with configurable angular resolution.
     *
     * @param radius finite positive radius
     * @param segments angular segments, at least three
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or the requested arrays exceed
     *     Java array limits
     */
    public static BufferGeometry create(float radius, int segments) {
        return create(radius, segments, 0.0f, TWO_PI);
    }

    /**
     * Creates a circular sector with configurable angular resolution and extent.
     *
     * <p>The center is the first vertex. Perimeter vertices include both ends of the sector, which
     * gives a complete circle a texture-safe duplicated seam.
     *
     * @param radius finite positive radius
     * @param segments angular segments, at least three
     * @param startAngle finite start angle in radians
     * @param angleLength finite positive angular length no greater than one revolution
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or the requested arrays exceed
     *     Java array limits
     */
    public static BufferGeometry create(float radius, int segments, float startAngle, float angleLength) {
        float validRadius = Preconditions.requirePositive(radius, "radius");
        int validSegments = Preconditions.requireInRange(segments, 3, Integer.MAX_VALUE, "segments");
        float validStartAngle = Preconditions.requireFinite(startAngle, "startAngle");
        float validAngleLength = Preconditions.requireInRange(angleLength, Float.MIN_VALUE, TWO_PI, "angleLength");

        long vertexCount = validSegments + 2L;
        int positionLength = Preconditions.requireArrayLength(vertexCount, 3, "position and normal");
        int textureCoordinateLength = Preconditions.requireArrayLength(vertexCount, 2, "texture-coordinate");
        int indexCount = Preconditions.requireArrayLength(validSegments, 3, "index");
        float[] positions = new float[positionLength];
        float[] normals = new float[positionLength];
        float[] textureCoordinates = new float[textureCoordinateLength];
        normals[2] = 1.0f;
        textureCoordinates[0] = 0.5f;
        textureCoordinates[1] = 0.5f;

        int positionOffset = 3;
        int textureCoordinateOffset = 2;
        for (int segment = 0; segment <= validSegments; segment++) {
            float angularCoordinate = (float) segment / validSegments;
            double angle = validStartAngle + angularCoordinate * validAngleLength;
            float cosine = (float) Math.cos(angle);
            float sine = (float) Math.sin(angle);
            positions[positionOffset] = validRadius * cosine;
            normals[positionOffset++] = 0.0f;
            positions[positionOffset] = validRadius * sine;
            normals[positionOffset++] = 0.0f;
            positions[positionOffset] = 0.0f;
            normals[positionOffset++] = 1.0f;
            textureCoordinates[textureCoordinateOffset++] = 0.5f + cosine * 0.5f;
            textureCoordinates[textureCoordinateOffset++] = 0.5f + sine * 0.5f;
        }

        int[] indices = new int[indexCount];
        int indexOffset = 0;
        for (int segment = 0; segment < validSegments; segment++) {
            indices[indexOffset++] = 0;
            indices[indexOffset++] = segment + 1;
            indices[indexOffset++] = segment + 2;
        }
        return BufferGeometryFactorySupport.create(positions, normals, textureCoordinates, indices);
    }
}
