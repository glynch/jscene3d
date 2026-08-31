/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.helpers;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import java.util.Arrays;
import java.util.Objects;

/** Square XZ-plane reference grid rendered as independent line segments. */
public final class GridHelper extends GeneratedLineSegmentsHelper {
    private static final float DEFAULT_SIZE = 10.0f;
    private static final int DEFAULT_DIVISIONS = 10;
    private static final Color DEFAULT_CENTER_LINE_COLOR = Color.srgb(0x444444);
    private static final Color DEFAULT_GRID_COLOR = Color.srgb(0x888888);

    /** Creates a ten-unit grid with ten divisions and standard gray colors. */
    public GridHelper() {
        this(DEFAULT_SIZE, DEFAULT_DIVISIONS);
    }

    /**
     * Creates a grid with standard gray center and grid-line colors.
     *
     * @param size finite positive side length
     * @param divisions positive number of divisions per side
     * @throws IllegalArgumentException if a numeric argument is invalid or exceeds Java array limits
     */
    public GridHelper(float size, int divisions) {
        this(size, divisions, DEFAULT_CENTER_LINE_COLOR, DEFAULT_GRID_COLOR);
    }

    /**
     * Creates a grid with explicit center and grid-line colors.
     *
     * <p>The helper owns its generated geometry and material. Close the helper rather than those
     * resources individually. Their replacement is unsupported. An odd division count has no line
     * exactly at the origin, so every line uses {@code gridColor}.
     *
     * @param size finite positive side length
     * @param divisions positive number of divisions per side
     * @param centerLineColor center-line color used when divisions are even
     * @param gridColor ordinary grid-line color
     * @throws NullPointerException if a color is {@code null}
     * @throws IllegalArgumentException if a numeric argument is invalid or exceeds Java array limits
     */
    public GridHelper(float size, int divisions, Color centerLineColor, Color gridColor) {
        super(createGeometry(size, divisions, centerLineColor, gridColor));
    }

    /** Generates positions and colors for both line directions in the XZ plane. */
    private static BufferGeometry createGeometry(float size, int divisions, Color centerLineColor, Color gridColor) {
        float validSize = Preconditions.requirePositive(size, "size");
        int validDivisions = Preconditions.requirePositive(divisions, "divisions");
        Color validCenterLineColor = Objects.requireNonNull(centerLineColor, "centerLineColor");
        Color validGridColor = Objects.requireNonNull(gridColor, "gridColor");
        long vertexCount = (validDivisions + 1L) * 4L;
        float[] positions = new float[Preconditions.requireArrayLength(vertexCount, 3, "grid position")];
        Color[] colors = new Color[Preconditions.requireArrayLength(vertexCount, 1, "grid color")];
        float halfSize = validSize * 0.5f;
        float step = validSize / validDivisions;

        for (int index = 0; index <= validDivisions; index++) {
            float coordinate = -halfSize + index * step;
            int positionOffset = index * 12;
            positions[positionOffset] = -halfSize;
            positions[positionOffset + 2] = coordinate;
            positions[positionOffset + 3] = halfSize;
            positions[positionOffset + 5] = coordinate;
            positions[positionOffset + 6] = coordinate;
            positions[positionOffset + 8] = -halfSize;
            positions[positionOffset + 9] = coordinate;
            positions[positionOffset + 11] = halfSize;

            Color color =
                    validDivisions % 2 == 0 && index == validDivisions / 2 ? validCenterLineColor : validGridColor;
            Arrays.fill(colors, index * 4, index * 4 + 4, color);
        }
        return BufferGeometry.builder()
                .positions(positions)
                .vertexColors(colors)
                .build();
    }
}
