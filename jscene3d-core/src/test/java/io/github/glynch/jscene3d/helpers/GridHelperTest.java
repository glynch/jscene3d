/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class GridHelperTest {
    @Test
    void createsCenteredEvenGridWithDistinctCenterLines() {
        try (GridHelper helper = new GridHelper(2.0f, 2, Color.RED, Color.BLUE)) {
            BufferAttribute positions = Objects.requireNonNull(helper.geometry().attribute(BufferGeometry.POSITION));
            BufferAttribute colors = Objects.requireNonNull(helper.geometry().attribute(BufferGeometry.COLOR));

            assertThat(positions.count()).isEqualTo(12);
            assertThat(positions.toArray())
                    .containsExactly(
                            -1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f,
                            0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                            1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f);
            assertVertexColor(colors, 0, Color.BLUE);
            assertVertexColor(colors, 4, Color.RED);
            assertVertexColor(colors, 8, Color.BLUE);
            assertThat(helper.material().usesVertexColors()).isTrue();
        }
    }

    @Test
    void createsDocumentedDefaultsAndUsesNoCenterColorForOddDivisions() {
        try (GridHelper defaultGrid = new GridHelper();
                GridHelper oddGrid = new GridHelper(3.0f, 3, Color.RED, Color.BLUE)) {
            assertThat(defaultGrid.geometry().vertexCount()).isEqualTo(44);

            BufferAttribute oddColors =
                    Objects.requireNonNull(oddGrid.geometry().attribute(BufferGeometry.COLOR));
            for (int vertexIndex = 0; vertexIndex < oddColors.count(); vertexIndex++) {
                assertVertexColor(oddColors, vertexIndex, Color.BLUE);
            }
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsInvalidConstructionArguments() {
        assertThatIllegalArgumentException().isThrownBy(() -> new GridHelper(0.0f, 10));
        assertThatIllegalArgumentException().isThrownBy(() -> new GridHelper(Float.NaN, 10));
        assertThatIllegalArgumentException().isThrownBy(() -> new GridHelper(10.0f, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new GridHelper(10.0f, Integer.MAX_VALUE));
        assertThatNullPointerException().isThrownBy(() -> new GridHelper(10.0f, 10, null, Color.WHITE));
        assertThatNullPointerException().isThrownBy(() -> new GridHelper(10.0f, 10, Color.WHITE, null));
    }

    @Test
    void ownsLifecycleAndRejectsResourceReplacement() {
        GridHelper helper = new GridHelper(2.0f, 2);
        BufferGeometry ownedGeometry = helper.geometry();
        LineBasicMaterial ownedMaterial = helper.material();
        try (BufferGeometry replacementGeometry = BufferGeometry.builder()
                        .positions(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
                        .build();
                LineBasicMaterial replacementMaterial = new LineBasicMaterial()) {
            assertThatThrownBy(() -> helper.setGeometry(replacementGeometry))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("GridHelper owns its geometry; replacement is unsupported");
            assertThatThrownBy(() -> helper.setMaterial(replacementMaterial))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("GridHelper owns its material; replacement is unsupported");
        }

        helper.close();

        assertThat(helper.isClosed()).isTrue();
        assertThat(ownedGeometry.isClosed()).isTrue();
        assertThat(ownedMaterial.isClosed()).isTrue();
    }

    /** Verifies one RGB vertex color against an immutable color value. */
    private static void assertVertexColor(BufferAttribute colors, int vertexIndex, Color expected) {
        assertThat(colors.value(vertexIndex, 0)).isEqualTo(expected.red());
        assertThat(colors.value(vertexIndex, 1)).isEqualTo(expected.green());
        assertThat(colors.value(vertexIndex, 2)).isEqualTo(expected.blue());
    }
}
