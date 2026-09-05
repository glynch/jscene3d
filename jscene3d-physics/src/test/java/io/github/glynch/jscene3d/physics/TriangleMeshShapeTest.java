/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class TriangleMeshShapeTest {
    private static final float[] POSITIONS = {-1.0F, 0.0F, -1.0F, 1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, -1.0F, 0.0F, 1.0F
    };
    private static final int[] INDICES = {0, 2, 1, 0, 3, 2};

    @Test
    void ownsIndexedGeometryByValue() {
        float[] positions = POSITIONS.clone();
        int[] indices = INDICES.clone();
        TriangleMeshShape mesh = new TriangleMeshShape(positions, indices);

        positions[0] = 99.0F;
        indices[0] = 3;

        assertThat(mesh.vertexCount()).isEqualTo(4);
        assertThat(mesh.triangleCount()).isEqualTo(2);
        assertThat(mesh.vertex(0, new Vector3f())).isEqualTo(new Vector3f(-1.0F, 0.0F, -1.0F));
        assertThat(mesh.index(0)).isZero();
        assertThat(mesh).isEqualTo(new TriangleMeshShape(POSITIONS, INDICES));
        assertThat(mesh.hashCode()).isEqualTo(new TriangleMeshShape(POSITIONS, INDICES).hashCode());
        assertThat(mesh.toString()).contains("positions=", "indices=");
    }

    @Test
    void rejectsMalformedAndDegenerateGeometry() {
        float[] incompletePositions = {0.0F, 0.0F, 0.0F};
        float[] nonFinitePositions = POSITIONS.clone();
        nonFinitePositions[0] = Float.NaN;
        int[] incompleteIndices = {0, 1};
        int[] invalidIndices = {0, 1, 9};
        float[] collinearPositions = {0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F};
        int[] oneTriangle = {0, 1, 2};

        assertThatThrownBy(() -> new TriangleMeshShape(incompletePositions, oneTriangle))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TriangleMeshShape(nonFinitePositions, INDICES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TriangleMeshShape(POSITIONS, incompleteIndices))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TriangleMeshShape(POSITIONS, invalidIndices))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TriangleMeshShape(collinearPositions, oneTriangle))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
