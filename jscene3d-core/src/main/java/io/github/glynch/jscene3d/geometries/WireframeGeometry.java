/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Creates line-segment geometry containing every unique triangle edge of another geometry. */
public final class WireframeGeometry {
    private WireframeGeometry() {}

    /**
     * Creates an independent wireframe snapshot of the source geometry's active draw range.
     *
     * <p>Edges are deduplicated by vertex index. Position data is copied, and subsequent changes
     * to either geometry do not affect the other.
     *
     * @param source triangle geometry to convert
     * @return indexed line-segment geometry
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if positions are absent or the draw range is not composed
     *     of complete triangles
     * @throws IllegalStateException if {@code source} is closed
     */
    public static BufferGeometry create(BufferGeometry source) {
        BufferGeometry validSource = Objects.requireNonNull(source, "source");
        BufferAttribute positions = validSource.attribute(BufferGeometry.POSITION);
        if (positions == null) {
            throw new IllegalArgumentException("source geometry must have a position attribute");
        }

        int start = validSource.drawRangeStart();
        int count = validSource.drawRangeCount();
        if (count % 3 != 0) {
            throw new IllegalArgumentException("source draw range must contain complete triangles: " + count);
        }

        IndexBuffer sourceIndex = validSource.index();
        Set<Long> edgeKeys = new LinkedHashSet<>();
        for (int offset = start; offset < start + count; offset += 3) {
            int first = vertexIndex(sourceIndex, offset);
            int second = vertexIndex(sourceIndex, offset + 1);
            int third = vertexIndex(sourceIndex, offset + 2);
            edgeKeys.add(edgeKey(first, second));
            edgeKeys.add(edgeKey(second, third));
            edgeKeys.add(edgeKey(third, first));
        }

        int[] edges = new int[Math.multiplyExact(edgeKeys.size(), 2)];
        int edgeOffset = 0;
        for (Long edgeKey : edgeKeys) {
            long packedEdge = edgeKey.longValue();
            edges[edgeOffset++] = (int) (packedEdge >>> Integer.SIZE);
            edges[edgeOffset++] = (int) packedEdge;
        }

        BufferGeometry wireframe = new BufferGeometry();
        wireframe.setAttribute(
                BufferGeometry.POSITION,
                BufferAttribute.of(positions.toArray(), positions.itemSize(), positions.usage()));
        wireframe.setIndex(IndexBuffer.of(edges));
        wireframe.computeBoundingBox();
        wireframe.computeBoundingSphere();
        return wireframe;
    }

    /** Returns the source vertex index for one draw-range element. */
    private static int vertexIndex(IndexBuffer index, int elementIndex) {
        return index == null ? elementIndex : index.value(elementIndex);
    }

    /** Packs an undirected edge into an ordered 64-bit key. */
    private static long edgeKey(int first, int second) {
        int minimum = Math.min(first, second);
        int maximum = Math.max(first, second);
        return ((long) minimum << Integer.SIZE) | Integer.toUnsignedLong(maximum);
    }
}
