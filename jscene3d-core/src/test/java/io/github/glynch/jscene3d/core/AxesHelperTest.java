/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Objects;
import org.junit.jupiter.api.Test;

final class AxesHelperTest {
    @Test
    void createsOwnedVertexColoredPositiveAxes() {
        try (AxesHelper helper = new AxesHelper(2.0f)) {
            BufferAttribute positions = Objects.requireNonNull(helper.geometry().attribute(BufferGeometry.POSITION));
            BufferAttribute colors = Objects.requireNonNull(helper.geometry().attribute(BufferGeometry.COLOR));

            assertThat(helper).isInstanceOf(LineSegments.class);
            assertThat(positions.toArray())
                    .containsExactly(
                            0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                            0.0f, 0.0f, 2.0f);
            assertThat(colors.toArray())
                    .containsExactly(
                            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
                            0.0f, 0.0f, 1.0f);
            assertThat(helper.material().usesVertexColors()).isTrue();
            assertThat(helper.isClosed()).isFalse();
        }
    }

    @Test
    void appliesUnitDefaultAndRejectsInvalidSizes() {
        try (AxesHelper helper = new AxesHelper()) {
            BufferAttribute positions = Objects.requireNonNull(helper.geometry().attribute(BufferGeometry.POSITION));
            assertThat(positions.value(1, 0)).isEqualTo(1.0f);
            assertThat(positions.value(3, 1)).isEqualTo(1.0f);
            assertThat(positions.value(5, 2)).isEqualTo(1.0f);
        }

        assertThatIllegalArgumentException().isThrownBy(() -> new AxesHelper(0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new AxesHelper(Float.NaN));
    }

    @Test
    void ownsLifecycleAndRejectsResourceReplacement() {
        AxesHelper helper = new AxesHelper();
        BufferGeometry ownedGeometry = helper.geometry();
        LineBasicMaterial ownedMaterial = helper.material();
        try (BufferGeometry replacementGeometry = BufferGeometry.builder()
                        .positions(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
                        .build();
                LineBasicMaterial replacementMaterial = new LineBasicMaterial()) {
            assertThatThrownBy(() -> helper.setGeometry(replacementGeometry))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("AxesHelper owns its geometry; replacement is unsupported");
            assertThatThrownBy(() -> helper.setMaterial(replacementMaterial))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("AxesHelper owns its material; replacement is unsupported");
        }

        helper.close();
        helper.close();

        assertThat(helper.isClosed()).isTrue();
        assertThat(ownedGeometry.isClosed()).isTrue();
        assertThat(ownedMaterial.isClosed()).isTrue();
    }
}
