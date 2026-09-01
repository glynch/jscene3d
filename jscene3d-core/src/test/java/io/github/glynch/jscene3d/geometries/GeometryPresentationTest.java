/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Objects;
import org.junit.jupiter.api.Test;

class GeometryPresentationTest {
    @Test
    void wireframeDeduplicatesSharedIndexedEdges() {
        try (BufferGeometry source = indexedQuad();
                BufferGeometry wireframe = WireframeGeometry.create(source)) {
            assertThat(Objects.requireNonNull(wireframe.index()).toArray())
                    .containsExactly(0, 1, 1, 2, 0, 2, 2, 3, 0, 3);
            assertThat(Objects.requireNonNull(wireframe.attribute(BufferGeometry.POSITION))
                            .toArray())
                    .containsExactly(Objects.requireNonNull(source.attribute(BufferGeometry.POSITION))
                            .toArray());
        }
    }

    @Test
    void flatShadingExpandsAttributesAndCreatesFaceNormals() {
        try (BufferGeometry source = indexedQuad();
                BufferGeometry flat = FlatShadedGeometry.create(source)) {
            assertThat(flat.index()).isNull();
            assertThat(Objects.requireNonNull(flat.attribute(BufferGeometry.POSITION))
                            .count())
                    .isEqualTo(6);
            assertThat(Objects.requireNonNull(flat.attribute(BufferGeometry.UV)).toArray())
                    .containsExactly(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f);
            assertThat(Objects.requireNonNull(flat.attribute(BufferGeometry.NORMAL))
                            .toArray())
                    .containsOnly(0.0f, 1.0f);
        }
    }

    @Test
    void presentationFactoriesRejectIncompleteTriangles() {
        try (BufferGeometry source = indexedQuad()) {
            source.setDrawRange(0, 4);

            assertThatThrownBy(() -> WireframeGeometry.create(source))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("complete triangles");
            assertThatThrownBy(() -> FlatShadedGeometry.create(source))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("complete triangles");
        }
    }

    /** Creates two indexed triangles sharing one diagonal. */
    private static BufferGeometry indexedQuad() {
        return BufferGeometry.builder()
                .attribute(
                        BufferGeometry.POSITION,
                        BufferAttribute.of(
                                new float[] {
                                    0.0f, 0.0f, 0.0f,
                                    1.0f, 0.0f, 0.0f,
                                    1.0f, 1.0f, 0.0f,
                                    0.0f, 1.0f, 0.0f
                                },
                                3))
                .attribute(
                        BufferGeometry.UV,
                        BufferAttribute.of(new float[] {0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f}, 2))
                .index(IndexBuffer.of(new int[] {0, 1, 2, 0, 2, 3}))
                .build();
    }
}
