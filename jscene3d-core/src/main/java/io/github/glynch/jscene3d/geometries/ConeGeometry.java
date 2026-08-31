/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static io.github.glynch.jscene3d.math.Angles.TWO_PI;

import java.util.Objects;

/** Creates indexed cones centered on the Y axis. */
public final class ConeGeometry {
    private static final int DEFAULT_RADIAL_SEGMENTS = 32;
    private static final int DEFAULT_HEIGHT_SEGMENTS = 1;

    /** Prevents instantiation of this geometry factory. */
    private ConeGeometry() {
        throw new AssertionError("ConeGeometry cannot be instantiated");
    }

    /**
     * Creates a closed cone using 32 radial segments and one height segment.
     *
     * @param radius finite positive base radius
     * @param height finite positive height
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a dimension is invalid
     */
    public static BufferGeometry create(float radius, float height) {
        return create(radius, height, DEFAULT_RADIAL_SEGMENTS);
    }

    /**
     * Creates a closed cone with configurable radial resolution.
     *
     * @param radius finite positive base radius
     * @param height finite positive height
     * @param radialSegments radial segments, at least three
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or the requested arrays exceed
     *     Java array limits
     */
    public static BufferGeometry create(float radius, float height, int radialSegments) {
        return create(new Options(radius, height, radialSegments, DEFAULT_HEIGHT_SEGMENTS, false, 0.0f, TWO_PI));
    }

    /**
     * Creates a cone with configurable tessellation and base cap.
     *
     * @param radius finite positive base radius
     * @param height finite positive height
     * @param radialSegments radial segments, at least three
     * @param heightSegments height segments, at least one
     * @param openEnded whether to omit the base cap
     * @return new application-owned geometry
     * @throws IllegalArgumentException if a parameter is invalid or the requested arrays exceed
     *     Java array limits
     */
    public static BufferGeometry create(
            float radius, float height, int radialSegments, int heightSegments, boolean openEnded) {
        return create(new Options(radius, height, radialSegments, heightSegments, openEnded, 0.0f, TWO_PI));
    }

    /**
     * Creates cone geometry from a complete immutable option set.
     *
     * @param options geometry options
     * @return new application-owned geometry
     * @throws NullPointerException if {@code options} is {@code null}
     * @throws IllegalArgumentException if an option is invalid or the requested arrays exceed Java
     *     array limits
     */
    public static BufferGeometry create(Options options) {
        Options validOptions = Objects.requireNonNull(options, "options");
        return CylinderGeometry.create(new CylinderGeometry.Options(
                0.0f,
                validOptions.radius(),
                validOptions.height(),
                validOptions.radialSegments(),
                validOptions.heightSegments(),
                validOptions.openEnded(),
                validOptions.startAngle(),
                validOptions.angleLength()));
    }

    /**
     * Complete immutable cone-generation options.
     *
     * @param radius finite positive base radius
     * @param height finite positive height
     * @param radialSegments radial segments, at least three
     * @param heightSegments height segments, at least one
     * @param openEnded whether to omit the base cap
     * @param startAngle finite start angle in radians
     * @param angleLength finite positive angular length no greater than one revolution
     */
    public record Options(
            float radius,
            float height,
            int radialSegments,
            int heightSegments,
            boolean openEnded,
            float startAngle,
            float angleLength) {}
}
