/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

/** Generates area-weighted unit vertex normals for triangle geometry. */
final class GeometryNormalGenerator {
    /** Prevents instantiation of this geometry algorithm. */
    private GeometryNormalGenerator() {
        throw new AssertionError("GeometryNormalGenerator cannot be instantiated");
    }

    /** Returns a normal attribute covering every position in the supplied triangle geometry. */
    static BufferAttribute generate(BufferAttribute positions, IndexBuffer indices) {
        int elementCount = indices == null ? positions.count() : indices.count();
        if (elementCount % 3 != 0) {
            throw new IllegalArgumentException(
                    "triangle geometry element count must be divisible by 3: " + elementCount);
        }
        float[] normals = new float[Math.multiplyExact(positions.count(), 3)];
        for (int element = 0; element < elementCount; element += 3) {
            accumulateTriangle(positions, indices, element, normals);
        }
        normalize(normals);
        return BufferAttribute.of(normals, 3);
    }

    /** Adds one triangle's area-weighted face normal to each referenced vertex. */
    private static void accumulateTriangle(
            BufferAttribute positions, IndexBuffer indices, int element, float[] normals) {
        int first = vertexIndex(indices, element);
        int second = vertexIndex(indices, element + 1);
        int third = vertexIndex(indices, element + 2);
        float firstX = positions.value(first, 0);
        float firstY = positions.value(first, 1);
        float firstZ = positions.value(first, 2);
        float firstToSecondX = positions.value(second, 0) - firstX;
        float firstToSecondY = positions.value(second, 1) - firstY;
        float firstToSecondZ = positions.value(second, 2) - firstZ;
        float firstToThirdX = positions.value(third, 0) - firstX;
        float firstToThirdY = positions.value(third, 1) - firstY;
        float firstToThirdZ = positions.value(third, 2) - firstZ;
        float normalX = firstToSecondY * firstToThirdZ - firstToSecondZ * firstToThirdY;
        float normalY = firstToSecondZ * firstToThirdX - firstToSecondX * firstToThirdZ;
        float normalZ = firstToSecondX * firstToThirdY - firstToSecondY * firstToThirdX;
        addNormal(normals, first, normalX, normalY, normalZ);
        addNormal(normals, second, normalX, normalY, normalZ);
        addNormal(normals, third, normalX, normalY, normalZ);
    }

    /** Returns the vertex referenced by one draw element. */
    private static int vertexIndex(IndexBuffer indices, int element) {
        return indices == null ? element : indices.value(element);
    }

    /** Adds one face normal to a vertex's pending sum. */
    private static void addNormal(float[] normals, int vertex, float x, float y, float z) {
        int offset = vertex * 3;
        normals[offset] += x;
        normals[offset + 1] += y;
        normals[offset + 2] += z;
    }

    /** Normalizes every non-zero accumulated vertex normal in place. */
    private static void normalize(float[] normals) {
        for (int offset = 0; offset < normals.length; offset += 3) {
            float x = normals[offset];
            float y = normals[offset + 1];
            float z = normals[offset + 2];
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            if (length != 0.0f) {
                normals[offset] = x / length;
                normals[offset + 1] = y / length;
                normals[offset + 2] = z / length;
            }
        }
    }
}
