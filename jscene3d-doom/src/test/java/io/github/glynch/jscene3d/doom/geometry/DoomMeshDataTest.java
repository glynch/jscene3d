/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.geometry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/** Verifies the immutable renderer-independent Doom mesh contract. */
final class DoomMeshDataTest {
    private static final float[] POSITIONS = {0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F};
    private static final float[] NORMALS = {0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F};
    private static final float[] TEXTURE_COORDINATES = {0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F};
    private static final int[] INDICES = {0, 1, 2};

    /** Exposes complete vertex data without surrendering ownership of its arrays. */
    @Test
    void exposesDefensiveComponentCopies() {
        DoomMeshData mesh = mesh();
        float[] positions = mesh.positions();
        float[] normals = mesh.normals();
        float[] textureCoordinates = mesh.textureCoordinates();
        int[] indices = mesh.indices();

        positions[0] = 99.0F;
        normals[2] = 99.0F;
        textureCoordinates[0] = 99.0F;
        indices[0] = 2;

        assertThat(mesh.vertexCount()).isEqualTo(3);
        assertThat(mesh.triangleCount()).isEqualTo(1);
        assertThat(mesh.position(0)).containsExactly(0.0F, 0.0F, 0.0F);
        assertThat(mesh.normal(0)).containsExactly(0.0F, 0.0F, 1.0F);
        assertThat(mesh.textureCoordinate(0)).containsExactly(0.0F, 0.0F);
        assertThat(mesh.indices()).containsExactly(0, 1, 2);
    }

    /** Implements value equality for every primitive component array. */
    @Test
    void comparesByMeshContent() {
        DoomMeshData mesh = mesh();

        assertThat(mesh)
                .isEqualTo(mesh())
                .hasSameHashCodeAs(mesh())
                .isNotEqualTo(null)
                .isNotEqualTo("mesh")
                .isNotEqualTo(new DoomMeshData(
                        new float[] {0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F},
                        NORMALS,
                        TEXTURE_COORDINATES,
                        INDICES))
                .hasToString("DoomMeshData[vertexCount=3, triangleCount=1]");
    }

    /** Rejects component arrays that cannot describe complete finite vertices. */
    @Test
    void rejectsInvalidVertexComponents() {
        float[] incompletePositions = {0.0F, 1.0F};
        float[] nonFinitePositions = {0.0F, Float.NaN, 0.0F};

        assertThatNullPointerException()
                .isThrownBy(() -> new DoomMeshData(null, NORMALS, TEXTURE_COORDINATES, INDICES))
                .withMessage("positions");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DoomMeshData(incompletePositions, NORMALS, TEXTURE_COORDINATES, INDICES))
                .withMessage("positions must contain complete components");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DoomMeshData(nonFinitePositions, new float[3], new float[2], new int[0]))
                .withMessage("positions must contain finite values");
    }

    /** Rejects mismatched vertex counts and malformed triangle indices. */
    @Test
    void rejectsInvalidTopology() {
        float[] oneNormal = {0.0F, 0.0F, 1.0F};
        int[] incompleteTriangle = {0, 1};
        int[] negativeIndex = {0, 1, -1};
        int[] excessiveIndex = {0, 1, 3};

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DoomMeshData(POSITIONS, oneNormal, TEXTURE_COORDINATES, INDICES))
                .withMessage("position, normal, and texture-coordinate counts must match");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DoomMeshData(POSITIONS, NORMALS, TEXTURE_COORDINATES, incompleteTriangle))
                .withMessage("indices must form complete triangles");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DoomMeshData(POSITIONS, NORMALS, TEXTURE_COORDINATES, negativeIndex))
                .withMessage("index is outside the vertex range: -1");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DoomMeshData(POSITIONS, NORMALS, TEXTURE_COORDINATES, excessiveIndex))
                .withMessage("index is outside the vertex range: 3");
    }

    /** Creates one complete triangle. */
    private static DoomMeshData mesh() {
        return new DoomMeshData(POSITIONS, NORMALS, TEXTURE_COORDINATES, INDICES);
    }
}
