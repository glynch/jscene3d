/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Independent line segments formed from successive pairs of geometry elements. */
public final class LineSegments extends Line {
    /**
     * Creates independent line segments retaining shared geometry and material references.
     *
     * <p>The selected draw range must contain an even number of elements so each element has one
     * partner.
     *
     * @param geometry open line geometry
     * @param material open line material
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is already closed
     */
    public LineSegments(BufferGeometry geometry, LineBasicMaterial material) {
        super(geometry, material);
    }
}
