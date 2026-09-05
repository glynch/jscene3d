/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import java.util.Arrays;
import java.util.Objects;

/** Immutable indexed collision data generated from one decoded Doom map. */
final class DoomStaticCollisionMesh {
    private final float[] positions;
    private final int[] indices;

    /** Copies complete flattened positions and triangle indices. */
    DoomStaticCollisionMesh(float[] positions, int[] indices) {
        this.positions = Objects.requireNonNull(positions, "positions").clone();
        this.indices = Objects.requireNonNull(indices, "indices").clone();
        if (this.positions.length < 9 || this.positions.length % 3 != 0) {
            throw new IllegalArgumentException("positions must contain at least three complete XYZ vertices");
        }
        if (this.indices.length < 3 || this.indices.length % 3 != 0) {
            throw new IllegalArgumentException("indices must contain at least one complete triangle");
        }
    }

    /** Returns a defensive copy of the flattened XYZ positions. */
    float[] positions() {
        return positions.clone();
    }

    /** Returns a defensive copy of the flattened triangle indices. */
    int[] indices() {
        return indices.clone();
    }

    /** Returns the number of generated triangles. */
    int triangleCount() {
        return indices.length / 3;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof DoomStaticCollisionMesh mesh
                        && Arrays.equals(positions, mesh.positions)
                        && Arrays.equals(indices, mesh.indices);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(positions) + Arrays.hashCode(indices);
    }

    @Override
    public String toString() {
        return "DoomStaticCollisionMesh[vertexCount=" + positions.length / 3 + ", triangleCount=" + triangleCount()
                + ']';
    }
}
