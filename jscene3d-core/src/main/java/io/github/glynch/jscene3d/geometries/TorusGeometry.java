/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.internal.Preconditions;

/** Creates indexed torus geometry around the Z axis. */
public final class TorusGeometry {
    private static final int DEFAULT_RADIAL_SEGMENTS = 12;
    private static final int DEFAULT_TUBULAR_SEGMENTS = 48;

    /** Prevents instantiation of this geometry factory. */
    private TorusGeometry() {
        throw new AssertionError("TorusGeometry cannot be instantiated");
    }

    /**
     * Creates a complete torus using 12 radial and 48 tubular segments.
     *
     * @param radius finite positive distance from the origin to the tube center
     * @param tubeRadius finite positive tube radius
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a radius is invalid
     */
    public static BufferGeometry create(float radius, float tubeRadius) {
        return create(radius, tubeRadius, DEFAULT_RADIAL_SEGMENTS, DEFAULT_TUBULAR_SEGMENTS);
    }

    /**
     * Creates a complete torus with configurable tessellation.
     *
     * @param radius finite positive distance from the origin to the tube center
     * @param tubeRadius finite positive tube radius
     * @param radialSegments tube cross-section segments, at least three
     * @param tubularSegments major-ring segments, at least three
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or the requested arrays exceed
     *     Java array limits
     */
    public static BufferGeometry create(float radius, float tubeRadius, int radialSegments, int tubularSegments) {
        return create(radius, tubeRadius, radialSegments, tubularSegments, TWO_PI);
    }

    /**
     * Creates a torus or torus arc with configurable tessellation.
     *
     * @param radius finite positive distance from the origin to the tube center
     * @param tubeRadius finite positive tube radius
     * @param radialSegments tube cross-section segments, at least three
     * @param tubularSegments major-ring segments, at least three
     * @param arc finite positive major-ring angular length no greater than one revolution
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or the requested arrays exceed
     *     Java array limits
     */
    public static BufferGeometry create(
            float radius, float tubeRadius, int radialSegments, int tubularSegments, float arc) {
        float validRadius = Preconditions.requirePositive(radius, "radius");
        float validTubeRadius = Preconditions.requirePositive(tubeRadius, "tubeRadius");
        int validRadialSegments = Preconditions.requireInRange(radialSegments, 3, Integer.MAX_VALUE, "radialSegments");
        int validTubularSegments =
                Preconditions.requireInRange(tubularSegments, 3, Integer.MAX_VALUE, "tubularSegments");
        float validArc = Preconditions.requireInRange(arc, Float.MIN_VALUE, TWO_PI, "arc");
        long vertexCount = (validRadialSegments + 1L) * (validTubularSegments + 1L);
        long quadCount = (long) validRadialSegments * validTubularSegments;
        int positionLength = Preconditions.requireArrayLength(vertexCount, 3, "position and normal");
        int textureCoordinateLength = Preconditions.requireArrayLength(vertexCount, 2, "texture-coordinate");
        int indexCount = Preconditions.requireArrayLength(quadCount, 6, "index");

        float[] positions = new float[positionLength];
        float[] normals = new float[positionLength];
        float[] textureCoordinates = new float[textureCoordinateLength];
        int positionOffset = 0;
        int textureCoordinateOffset = 0;
        for (int tubularSegment = 0; tubularSegment <= validTubularSegments; tubularSegment++) {
            float u = (float) tubularSegment / validTubularSegments;
            double majorAngle = u * validArc;
            float majorCosine = (float) Math.cos(majorAngle);
            float majorSine = (float) Math.sin(majorAngle);
            for (int radialSegment = 0; radialSegment <= validRadialSegments; radialSegment++) {
                float v = (float) radialSegment / validRadialSegments;
                double tubeAngle = v * TWO_PI;
                float tubeCosine = (float) Math.cos(tubeAngle);
                float tubeSine = (float) Math.sin(tubeAngle);
                float normalX = tubeCosine * majorCosine;
                float normalY = tubeCosine * majorSine;
                float normalZ = tubeSine;
                float distance = validRadius + validTubeRadius * tubeCosine;
                positions[positionOffset] = distance * majorCosine;
                normals[positionOffset++] = normalX;
                positions[positionOffset] = distance * majorSine;
                normals[positionOffset++] = normalY;
                positions[positionOffset] = validTubeRadius * tubeSine;
                normals[positionOffset++] = normalZ;
                textureCoordinates[textureCoordinateOffset++] = u;
                textureCoordinates[textureCoordinateOffset++] = v;
            }
        }

        int[] indices = new int[indexCount];
        int indexOffset = 0;
        int rowLength = validRadialSegments + 1;
        for (int tubularSegment = 0; tubularSegment < validTubularSegments; tubularSegment++) {
            for (int radialSegment = 0; radialSegment < validRadialSegments; radialSegment++) {
                int current = tubularSegment * rowLength + radialSegment;
                int nextTube = current + rowLength;
                indices[indexOffset++] = current;
                indices[indexOffset++] = nextTube;
                indices[indexOffset++] = current + 1;
                indices[indexOffset++] = nextTube;
                indices[indexOffset++] = nextTube + 1;
                indices[indexOffset++] = current + 1;
            }
        }
        return BufferGeometryFactorySupport.create(positions, normals, textureCoordinates, indices);
    }
}
