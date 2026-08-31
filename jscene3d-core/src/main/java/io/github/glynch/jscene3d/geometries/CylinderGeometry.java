/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.Objects;

/** Creates indexed cylinders and truncated cones centered on the Y axis. */
public final class CylinderGeometry {
    private static final int DEFAULT_RADIAL_SEGMENTS = 32;
    private static final int DEFAULT_HEIGHT_SEGMENTS = 1;

    /** Prevents instantiation of this geometry factory. */
    private CylinderGeometry() {
        throw new AssertionError("CylinderGeometry cannot be instantiated");
    }

    /**
     * Creates a closed cylinder using 32 radial segments and one height segment.
     *
     * @param radius finite positive radius
     * @param height finite positive height
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a dimension is invalid
     */
    public static BufferGeometry create(float radius, float height) {
        return create(radius, radius, height);
    }

    /**
     * Creates a closed cylinder or truncated cone using default segment counts.
     *
     * @param topRadius finite non-negative top radius
     * @param bottomRadius finite non-negative bottom radius
     * @param height finite positive height
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a dimension is invalid or both radii are zero
     */
    public static BufferGeometry create(float topRadius, float bottomRadius, float height) {
        return create(new Options(
                topRadius,
                bottomRadius,
                height,
                DEFAULT_RADIAL_SEGMENTS,
                DEFAULT_HEIGHT_SEGMENTS,
                false,
                0.0f,
                TWO_PI));
    }

    /**
     * Creates a cylinder or truncated cone with configurable tessellation and caps.
     *
     * @param topRadius finite non-negative top radius
     * @param bottomRadius finite non-negative bottom radius
     * @param height finite positive height
     * @param radialSegments radial segments, at least three
     * @param heightSegments height segments, at least one
     * @param openEnded whether to omit both end caps
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid, both radii are zero, or the
     *     requested arrays exceed Java array limits
     */
    public static BufferGeometry create(
            float topRadius,
            float bottomRadius,
            float height,
            int radialSegments,
            int heightSegments,
            boolean openEnded) {
        return create(
                new Options(topRadius, bottomRadius, height, radialSegments, heightSegments, openEnded, 0.0f, TWO_PI));
    }

    /**
     * Creates geometry from a complete immutable option set.
     *
     * @param options geometry options
     * @return new application-owned geometry
     * @throws NullPointerException if {@code options} is {@code null}
     * @throws IllegalArgumentException if an option is invalid, both radii are zero, or the
     *     requested arrays exceed Java array limits
     */
    public static BufferGeometry create(Options options) {
        Options validOptions = validate(Objects.requireNonNull(options, "options"));
        int radialSegments = validOptions.radialSegments();
        int heightSegments = validOptions.heightSegments();
        float topRadius = validOptions.topRadius();
        float bottomRadius = validOptions.bottomRadius();
        boolean includeTop = !validOptions.openEnded() && topRadius > 0.0f;
        boolean includeBottom = !validOptions.openEnded() && bottomRadius > 0.0f;
        long sideVertexCount = (radialSegments + 1L) * (heightSegments + 1L);
        long capVertexCount = (includeTop ? radialSegments + 2L : 0L) + (includeBottom ? radialSegments + 2L : 0L);
        long vertexCount = sideVertexCount + capVertexCount;
        long sideTriangleCount = radialSegments
                * (2L * heightSegments - (topRadius == 0.0f ? 1L : 0L) - (bottomRadius == 0.0f ? 1L : 0L));
        long capTriangleCount = (includeTop ? radialSegments : 0L) + (includeBottom ? radialSegments : 0L);
        int positionLength = Preconditions.requireArrayLength(vertexCount, 3, "position and normal");
        int textureCoordinateLength = Preconditions.requireArrayLength(vertexCount, 2, "texture-coordinate");
        int indexCount = Preconditions.requireArrayLength(sideTriangleCount + capTriangleCount, 3, "index");

        GeometryData data = new GeometryData(
                new float[positionLength],
                new float[positionLength],
                new float[textureCoordinateLength],
                new int[indexCount]);
        generateSide(data, validOptions);
        if (includeTop) {
            generateCap(data, validOptions, true);
        }
        if (includeBottom) {
            generateCap(data, validOptions, false);
        }
        return BufferGeometryFactorySupport.create(data.positions, data.normals, data.textureCoordinates, data.indices);
    }

    /** Generates the curved surface and advances the packed data offsets. */
    private static void generateSide(GeometryData data, Options options) {
        int radialSegments = options.radialSegments();
        int heightSegments = options.heightSegments();
        float slope = (options.bottomRadius() - options.topRadius()) / options.height();
        float inverseNormalLength = 1.0f / (float) Math.sqrt(1.0f + slope * slope);
        for (int heightSegment = 0; heightSegment <= heightSegments; heightSegment++) {
            float verticalCoordinate = (float) heightSegment / heightSegments;
            float radius = options.topRadius() + (options.bottomRadius() - options.topRadius()) * verticalCoordinate;
            float y = options.height() * (0.5f - verticalCoordinate);
            for (int radialSegment = 0; radialSegment <= radialSegments; radialSegment++) {
                float angularCoordinate = (float) radialSegment / radialSegments;
                double angle = options.startAngle() + angularCoordinate * options.angleLength();
                float cosine = (float) Math.cos(angle);
                float sine = (float) Math.sin(angle);
                data.addVertex(
                        radius * cosine,
                        y,
                        radius * sine,
                        cosine * inverseNormalLength,
                        slope * inverseNormalLength,
                        sine * inverseNormalLength);
                data.addTextureCoordinate(angularCoordinate, 1.0f - verticalCoordinate);
            }
        }

        int rowLength = radialSegments + 1;
        for (int heightSegment = 0; heightSegment < heightSegments; heightSegment++) {
            boolean upperCollapsed = heightSegment == 0 && options.topRadius() == 0.0f;
            boolean lowerCollapsed = heightSegment == heightSegments - 1 && options.bottomRadius() == 0.0f;
            for (int radialSegment = 0; radialSegment < radialSegments; radialSegment++) {
                int upperCurrent = heightSegment * rowLength + radialSegment;
                int upperNext = upperCurrent + 1;
                int lowerCurrent = upperCurrent + rowLength;
                int lowerNext = lowerCurrent + 1;
                if (!upperCollapsed) {
                    data.addTriangle(upperCurrent, upperNext, lowerNext);
                }
                if (!lowerCollapsed) {
                    data.addTriangle(upperCurrent, lowerNext, lowerCurrent);
                }
            }
        }
    }

    /** Generates one flat cap and advances the packed data offsets. */
    private static void generateCap(GeometryData data, Options options, boolean top) {
        int radialSegments = options.radialSegments();
        float radius = top ? options.topRadius() : options.bottomRadius();
        float y = (top ? 0.5f : -0.5f) * options.height();
        float normalY = top ? 1.0f : -1.0f;
        int centerIndex = data.vertexCount();
        data.addVertex(0.0f, y, 0.0f, 0.0f, normalY, 0.0f);
        data.addTextureCoordinate(0.5f, 0.5f);
        int ringStart = data.vertexCount();
        for (int radialSegment = 0; radialSegment <= radialSegments; radialSegment++) {
            float angularCoordinate = (float) radialSegment / radialSegments;
            double angle = options.startAngle() + angularCoordinate * options.angleLength();
            float cosine = (float) Math.cos(angle);
            float sine = (float) Math.sin(angle);
            data.addVertex(radius * cosine, y, radius * sine, 0.0f, normalY, 0.0f);
            data.addTextureCoordinate(0.5f + cosine * 0.5f, 0.5f + sine * 0.5f);
        }
        for (int radialSegment = 0; radialSegment < radialSegments; radialSegment++) {
            int current = ringStart + radialSegment;
            int next = current + 1;
            if (top) {
                data.addTriangle(centerIndex, next, current);
            } else {
                data.addTriangle(centerIndex, current, next);
            }
        }
    }

    /** Validates an option set and returns it unchanged. */
    private static Options validate(Options options) {
        float topRadius = Preconditions.requireNonNegative(options.topRadius(), "topRadius");
        float bottomRadius = Preconditions.requireNonNegative(options.bottomRadius(), "bottomRadius");
        if (topRadius == 0.0f && bottomRadius == 0.0f) {
            throw new IllegalArgumentException("topRadius and bottomRadius must not both be zero");
        }
        Preconditions.requirePositive(options.height(), "height");
        Preconditions.requireInRange(options.radialSegments(), 3, Integer.MAX_VALUE, "radialSegments");
        Preconditions.requirePositive(options.heightSegments(), "heightSegments");
        Preconditions.requireFinite(options.startAngle(), "startAngle");
        Preconditions.requireInRange(options.angleLength(), Float.MIN_VALUE, TWO_PI, "angleLength");
        return options;
    }

    /** Mutable assembly buffers and offsets scoped to one factory invocation. */
    private static final class GeometryData {
        private final float[] positions;
        private final float[] normals;
        private final float[] textureCoordinates;
        private final int[] indices;
        private int positionOffset;
        private int textureCoordinateOffset;
        private int indexOffset;

        private GeometryData(float[] positions, float[] normals, float[] textureCoordinates, int[] indices) {
            this.positions = positions;
            this.normals = normals;
            this.textureCoordinates = textureCoordinates;
            this.indices = indices;
        }

        /** Returns the number of vertices appended so far. */
        private int vertexCount() {
            return positionOffset / 3;
        }

        /** Appends one packed vertex. */
        private void addVertex(float x, float y, float z, float normalX, float normalY, float normalZ) {
            positions[positionOffset] = x;
            normals[positionOffset++] = normalX;
            positions[positionOffset] = y;
            normals[positionOffset++] = normalY;
            positions[positionOffset] = z;
            normals[positionOffset++] = normalZ;
        }

        /** Appends one packed texture coordinate. */
        private void addTextureCoordinate(float u, float v) {
            textureCoordinates[textureCoordinateOffset++] = u;
            textureCoordinates[textureCoordinateOffset++] = v;
        }

        /** Appends one triangle. */
        private void addTriangle(int first, int second, int third) {
            indices[indexOffset++] = first;
            indices[indexOffset++] = second;
            indices[indexOffset++] = third;
        }
    }

    /**
     * Complete immutable cylinder-generation options.
     *
     * @param topRadius finite non-negative top radius
     * @param bottomRadius finite non-negative bottom radius
     * @param height finite positive height
     * @param radialSegments radial segments, at least three
     * @param heightSegments height segments, at least one
     * @param openEnded whether to omit both end caps
     * @param startAngle finite start angle in radians
     * @param angleLength finite positive angular length no greater than one revolution
     */
    public record Options(
            float topRadius,
            float bottomRadius,
            float height,
            int radialSegments,
            int heightSegments,
            boolean openEnded,
            float startAngle,
            float angleLength) {}
}
