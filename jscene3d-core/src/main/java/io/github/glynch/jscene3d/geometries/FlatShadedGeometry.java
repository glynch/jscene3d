/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Creates non-indexed geometry with one constant normal per triangle face. */
public final class FlatShadedGeometry {
    private FlatShadedGeometry() {}

    /**
     * Creates an independent flat-shaded snapshot of the source geometry's active draw range.
     *
     * <p>Every source attribute except {@code normal} is expanded along the source index. New face
     * normals replace any source normals, so adjacent triangles never share normal values.
     *
     * @param source triangle geometry to convert
     * @return non-indexed flat-shaded geometry
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if positions are absent, the draw range is not composed of
     *     complete triangles, or a triangle is degenerate
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
        Map<String, BufferAttribute> expandedAttributes = new LinkedHashMap<>();
        for (Map.Entry<String, BufferAttribute> entry : validSource.attributes().entrySet()) {
            if (!BufferGeometry.NORMAL.equals(entry.getKey())) {
                expandedAttributes.put(entry.getKey(), expand(entry.getValue(), sourceIndex, start, count));
            }
        }

        BufferAttribute expandedPositions = expandedAttributes.get(BufferGeometry.POSITION);
        float[] normals = createFaceNormals(Objects.requireNonNull(expandedPositions, "expanded positions"));
        expandedAttributes.put(BufferGeometry.NORMAL, BufferAttribute.of(normals, 3));

        BufferGeometry result = new BufferGeometry();
        expandedAttributes.forEach(result::setAttribute);
        result.computeBoundingBox();
        result.computeBoundingSphere();
        return result;
    }

    /** Expands one source attribute into draw order. */
    private static BufferAttribute expand(BufferAttribute source, IndexBuffer index, int start, int count) {
        int itemSize = source.itemSize();
        float[] expanded = new float[Math.multiplyExact(count, itemSize)];
        for (int elementOffset = 0; elementOffset < count; elementOffset++) {
            int vertexIndex = index == null ? start + elementOffset : index.value(start + elementOffset);
            int targetOffset = elementOffset * itemSize;
            for (int component = 0; component < itemSize; component++) {
                expanded[targetOffset + component] = source.value(vertexIndex, component);
            }
        }
        return BufferAttribute.of(expanded, itemSize, source.usage());
    }

    /** Calculates and duplicates one unit normal for each expanded triangle. */
    private static float[] createFaceNormals(BufferAttribute positions) {
        float[] normals = new float[Math.multiplyExact(positions.count(), 3)];
        for (int vertex = 0; vertex < positions.count(); vertex += 3) {
            float ax = positions.value(vertex, 0);
            float ay = positions.value(vertex, 1);
            float az = positions.value(vertex, 2);
            float abx = positions.value(vertex + 1, 0) - ax;
            float aby = positions.value(vertex + 1, 1) - ay;
            float abz = positions.value(vertex + 1, 2) - az;
            float acx = positions.value(vertex + 2, 0) - ax;
            float acy = positions.value(vertex + 2, 1) - ay;
            float acz = positions.value(vertex + 2, 2) - az;
            float normalX = aby * acz - abz * acy;
            float normalY = abz * acx - abx * acz;
            float normalZ = abx * acy - aby * acx;
            float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            if (length == 0.0f) {
                throw new IllegalArgumentException(
                        "source geometry contains a degenerate triangle at vertex " + vertex);
            }
            writeNormal(normals, vertex, normalX / length, normalY / length, normalZ / length);
        }
        return normals;
    }

    /** Writes one face normal to all three vertices of a triangle. */
    private static void writeNormal(float[] normals, int vertex, float x, float y, float z) {
        for (int triangleVertex = 0; triangleVertex < 3; triangleVertex++) {
            int offset = (vertex + triangleVertex) * 3;
            normals[offset] = x;
            normals[offset + 1] = y;
            normals[offset + 2] = z;
        }
    }
}
