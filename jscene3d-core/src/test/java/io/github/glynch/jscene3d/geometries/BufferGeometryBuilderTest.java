/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class BufferGeometryBuilderTest {
    @Test
    void buildsStandardStaticAttributesIndicesAndDrawRange() {
        BufferGeometry.Builder builder = BufferGeometry.builder()
                .positions(-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
                .normals(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
                .uvs(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)
                .vertexColors(Color.RED, Color.GREEN, Color.BLUE)
                .attribute("weight", BufferAttribute.of(new float[] {0.25f, 0.5f, 0.75f}, 1))
                .indices(0, 1, 2)
                .drawRange(0, 3);

        try (BufferGeometry geometry = builder.build()) {
            assertThat(geometry.attributes().keySet())
                    .containsExactly(
                            BufferGeometry.POSITION,
                            BufferGeometry.NORMAL,
                            BufferGeometry.UV,
                            BufferGeometry.COLOR,
                            "weight");
            assertThat(geometry.vertexCount()).isEqualTo(3);
            assertThat(geometry.drawRangeCount()).isEqualTo(3);
            assertThat(geometry.hasExplicitDrawRange()).isTrue();
            assertThat(geometry.index()).isNotNull();

            BufferAttribute colors = Objects.requireNonNull(geometry.attribute(BufferGeometry.COLOR));
            assertThat(colors.itemSize()).isEqualTo(3);
            assertThat(colors.toArray()).containsExactly(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        }

        assertThatIllegalStateException().isThrownBy(builder::build);
    }

    @Test
    void copiesConvenienceDataWhenConfigured() {
        float[] positions = {-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
        int[] indices = {0, 1, 2};
        BufferGeometry.Builder builder =
                BufferGeometry.builder().positions(positions).indices(indices);

        positions[0] = 99.0f;
        indices[0] = 2;

        try (BufferGeometry geometry = builder.build()) {
            BufferAttribute configuredPositions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));
            IndexBuffer configuredIndices = Objects.requireNonNull(geometry.index());
            assertThat(configuredPositions.value(0, 0)).isEqualTo(-1.0f);
            assertThat(configuredIndices.value(0)).isZero();
        }
    }

    @Test
    void validatesCompleteConfigurationBeforeBuilding() {
        BufferGeometry.Builder mismatchedAttributes = BufferGeometry.builder()
                .positions(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
                .vertexColors(Color.RED, Color.GREEN);
        BufferGeometry.Builder missingPositions = BufferGeometry.builder().indices(0, 1, 2);
        BufferGeometry.Builder invalidIndex =
                BufferGeometry.builder().positions(0.0f, 0.0f, 0.0f).indices(1);
        BufferGeometry.Builder invalidRange =
                BufferGeometry.builder().positions(0.0f, 0.0f, 0.0f).drawRange(0, 2);

        assertThatIllegalArgumentException().isThrownBy(mismatchedAttributes::build);
        assertThatIllegalArgumentException().isThrownBy(missingPositions::build);
        assertThatIllegalArgumentException().isThrownBy(invalidIndex::build);
        assertThatIllegalArgumentException().isThrownBy(invalidRange::build);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullConvenienceArguments() {
        BufferGeometry.Builder builder = BufferGeometry.builder();
        BufferAttribute emptyAttribute = BufferAttribute.of(new float[0], 1);

        assertThatNullPointerException().isThrownBy(() -> builder.positions((float[]) null));
        assertThatNullPointerException().isThrownBy(() -> builder.vertexColors((Color[]) null));
        assertThatNullPointerException().isThrownBy(() -> builder.vertexColors(Color.RED, null));
        assertThatNullPointerException().isThrownBy(() -> builder.indices((int[]) null));
        assertThatNullPointerException().isThrownBy(() -> builder.attribute(null, emptyAttribute));
        assertThatNullPointerException().isThrownBy(() -> builder.attribute("custom", null));
        assertThatNullPointerException().isThrownBy(() -> builder.index(null));
    }
}
