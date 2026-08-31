/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import io.github.glynch.jscene3d.internal.Preconditions;

/** Creates indexed annular geometry in the XY plane facing positive Z. */
public final class RingGeometry {
    private static final int DEFAULT_SEGMENTS = 32;

    /** Prevents instantiation of this geometry factory. */
    private RingGeometry() {
        throw new AssertionError("RingGeometry cannot be instantiated");
    }

    /**
     * Creates a complete ring using 32 angular segments.
     *
     * <p>The first texture-coordinate component runs from zero at the inner edge to one at the
     * outer edge. The second component runs once around the circumference. This radial-angular
     * layout supports both circumferential textures and one-dimensional radial ring maps.
     *
     * @param innerRadius finite non-negative inner radius
     * @param outerRadius finite outer radius greater than {@code innerRadius}
     * @return new application-owned geometry
     * @throws IllegalArgumentException if either radius is invalid
     */
    public static BufferGeometry create(float innerRadius, float outerRadius) {
        return create(innerRadius, outerRadius, DEFAULT_SEGMENTS);
    }

    /**
     * Creates a complete ring with a configurable angular resolution.
     *
     * <p>The ring lies in the XY plane, is centered at the origin, and faces positive Z. The
     * duplicated seam vertices carry texture coordinates zero and one so repeating textures do
     * not interpolate across the seam.
     *
     * @param innerRadius finite non-negative inner radius
     * @param outerRadius finite outer radius greater than {@code innerRadius}
     * @param segments angular segments, at least three
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or the requested arrays exceed
     *     Java array limits
     */
    public static BufferGeometry create(float innerRadius, float outerRadius, int segments) {
        float validInnerRadius = Preconditions.requireNonNegative(innerRadius, "innerRadius");
        float validOuterRadius = Preconditions.requirePositive(outerRadius, "outerRadius");
        Preconditions.requireLessThan(validInnerRadius, "innerRadius", validOuterRadius, "outerRadius");
        int validSegments = Preconditions.requireInRange(segments, 3, Integer.MAX_VALUE, "segments");

        long vertexCount = 2L * (validSegments + 1L);
        int positionLength = Preconditions.requireArrayLength(vertexCount, 3, "position and normal");
        int textureCoordinateLength = Preconditions.requireArrayLength(vertexCount, 2, "texture-coordinate");
        int indexCount = Preconditions.requireArrayLength(validSegments, 6, "index");

        float[] positions = new float[positionLength];
        float[] normals = new float[positionLength];
        float[] textureCoordinates = new float[textureCoordinateLength];
        int positionOffset = 0;
        int textureCoordinateOffset = 0;
        for (int segment = 0; segment <= validSegments; segment++) {
            float angularCoordinate = (float) segment / validSegments;
            double angle = angularCoordinate * Math.PI * 2.0;
            float cosine = (float) Math.cos(angle);
            float sine = (float) Math.sin(angle);

            positionOffset =
                    addVertex(positions, normals, positionOffset, validInnerRadius * cosine, validInnerRadius * sine);
            textureCoordinates[textureCoordinateOffset++] = 0.0f;
            textureCoordinates[textureCoordinateOffset++] = angularCoordinate;

            positionOffset =
                    addVertex(positions, normals, positionOffset, validOuterRadius * cosine, validOuterRadius * sine);
            textureCoordinates[textureCoordinateOffset++] = 1.0f;
            textureCoordinates[textureCoordinateOffset++] = angularCoordinate;
        }

        int[] indices = new int[indexCount];
        int indexOffset = 0;
        for (int segment = 0; segment < validSegments; segment++) {
            int inner = segment * 2;
            int outer = inner + 1;
            int nextInner = inner + 2;
            int nextOuter = inner + 3;
            indices[indexOffset++] = inner;
            indices[indexOffset++] = outer;
            indices[indexOffset++] = nextOuter;
            indices[indexOffset++] = inner;
            indices[indexOffset++] = nextOuter;
            indices[indexOffset++] = nextInner;
        }
        return BufferGeometryFactorySupport.create(positions, normals, textureCoordinates, indices);
    }

    /** Appends one positive-Z vertex and returns the next packed component offset. */
    private static int addVertex(float[] positions, float[] normals, int offset, float x, float y) {
        positions[offset] = x;
        normals[offset++] = 0.0f;
        positions[offset] = y;
        normals[offset++] = 0.0f;
        positions[offset] = 0.0f;
        normals[offset++] = 1.0f;
        return offset;
    }
}
