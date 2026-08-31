/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

final class LineBasicMaterialTest {
    @Test
    void exposesDefaultsAndRecordsOnlyActualChanges() {
        try (LineBasicMaterial material = new LineBasicMaterial()) {
            assertThat(material.color()).isEqualTo(Color.WHITE);
            assertThat(material.usesVertexColors()).isFalse();
            assertThat(material.version()).isZero();

            material.setColor(Color.RED);
            material.setUsesVertexColors(true);

            assertThat(material.color()).isEqualTo(Color.RED);
            assertThat(material.usesVertexColors()).isTrue();
            assertThat(material.version()).isEqualTo(2L);

            material.setColor(Color.RED);
            material.setUsesVertexColors(true);
            assertThat(material.version()).isEqualTo(2L);
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsNullColorsAndUseAfterClosure() {
        assertThatNullPointerException().isThrownBy(() -> new LineBasicMaterial(null));

        LineBasicMaterial material = new LineBasicMaterial(Color.BLUE);
        assertThatNullPointerException().isThrownBy(() -> material.setColor(null));
        material.close();

        assertThatIllegalStateException().isThrownBy(material::color);
        assertThatIllegalStateException().isThrownBy(material::usesVertexColors);
        assertThatIllegalStateException().isThrownBy(() -> material.setColor(Color.RED));
        assertThatIllegalStateException().isThrownBy(() -> material.setUsesVertexColors(true));
    }
}
