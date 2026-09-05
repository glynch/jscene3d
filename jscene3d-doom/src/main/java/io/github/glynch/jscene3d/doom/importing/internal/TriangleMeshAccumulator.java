/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import java.util.Arrays;

/** Accumulates non-degenerate triangles into compact primitive artifact arrays. */
final class TriangleMeshAccumulator {
    private static final float MINIMUM_NORMAL_LENGTH_SQUARED = 1.0E-12F;
    private static final int INITIAL_POSITION_CAPACITY = 96;
    private static final int INITIAL_INDEX_CAPACITY = 32;

    private float[] positions = new float[INITIAL_POSITION_CAPACITY];
    private int[] indices = new int[INITIAL_INDEX_CAPACITY];
    private int positionCount;
    private int indexCount;

    /** Adds one triangle unless its three positions are coincident or collinear. */
    void addTriangle(Point3 first, Point3 second, Point3 third) {
        if (isDegenerate(first, second, third)) {
            return;
        }
        requirePositionCapacity(positionCount + 9);
        requireIndexCapacity(indexCount + 3);
        int firstIndex = positionCount / 3;
        addPosition(first);
        addPosition(second);
        addPosition(third);
        indices[indexCount++] = firstIndex;
        indices[indexCount++] = firstIndex + 1;
        indices[indexCount++] = firstIndex + 2;
    }

    /** Returns immutable copies trimmed to the accumulated content. */
    DoomStaticCollisionMesh build() {
        return new DoomStaticCollisionMesh(Arrays.copyOf(positions, positionCount), Arrays.copyOf(indices, indexCount));
    }

    /** Appends one XYZ position after capacity has been reserved. */
    private void addPosition(Point3 point) {
        positions[positionCount++] = point.x();
        positions[positionCount++] = point.y();
        positions[positionCount++] = point.z();
    }

    /** Expands the primitive position storage geometrically. */
    private void requirePositionCapacity(int required) {
        if (required > positions.length) {
            positions = Arrays.copyOf(positions, Math.max(required, positions.length * 2));
        }
    }

    /** Expands the primitive index storage geometrically. */
    private void requireIndexCapacity(int required) {
        if (required > indices.length) {
            indices = Arrays.copyOf(indices, Math.max(required, indices.length * 2));
        }
    }

    /** Detects triangles that cannot provide a stable collision plane. */
    private static boolean isDegenerate(Point3 first, Point3 second, Point3 third) {
        float firstX = second.x() - first.x();
        float firstY = second.y() - first.y();
        float firstZ = second.z() - first.z();
        float secondX = third.x() - first.x();
        float secondY = third.y() - first.y();
        float secondZ = third.z() - first.z();
        float normalX = firstY * secondZ - firstZ * secondY;
        float normalY = firstZ * secondX - firstX * secondZ;
        float normalZ = firstX * secondY - firstY * secondX;
        return normalX * normalX + normalY * normalY + normalZ * normalZ < MINIMUM_NORMAL_LENGTH_SQUARED;
    }

    /** One finite position in collision-resource local space. */
    record Point3(float x, float y, float z) {}
}
