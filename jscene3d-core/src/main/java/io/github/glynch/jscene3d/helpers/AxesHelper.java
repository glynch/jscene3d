/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.helpers;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;

/** Positive X, Y, and Z coordinate axes rendered as red, green, and blue segments. */
public final class AxesHelper extends GeneratedLineSegmentsHelper {
    /** Creates unit-length coordinate axes. */
    public AxesHelper() {
        this(1.0f);
    }

    /**
     * Creates coordinate axes extending from the origin by the supplied length.
     *
     * <p>The helper owns its generated geometry and material. Close the helper rather than those
     * resources individually. Their replacement is unsupported.
     *
     * @param size finite positive axis length
     * @throws IllegalArgumentException if {@code size} is not finite and positive
     */
    public AxesHelper(float size) {
        super(createGeometry(size));
    }

    /** Creates the six colored endpoints used by the three independent segments. */
    private static BufferGeometry createGeometry(float size) {
        float validSize = Preconditions.requirePositive(size, "size");
        return BufferGeometry.builder()
                .positions(
                        0.0f, 0.0f, 0.0f, validSize, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, validSize, 0.0f, 0.0f, 0.0f,
                        0.0f, 0.0f, 0.0f, validSize)
                .vertexColors(Color.RED, Color.RED, Color.GREEN, Color.GREEN, Color.BLUE, Color.BLUE)
                .build();
    }
}
