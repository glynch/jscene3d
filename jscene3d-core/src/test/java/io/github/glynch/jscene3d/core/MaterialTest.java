/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

final class MaterialTest {
    @Test
    void providesDocumentedBasicMaterialDefaults() {
        try (BasicMaterial material = new BasicMaterial()) {
            assertThat(material.color()).isEqualTo(Color.WHITE);
            assertThat(material.usesVertexColors()).isFalse();
            assertThat(material.visible()).isTrue();
            assertThat(material.opacity()).isEqualTo(1.0f);
            assertThat(material.transparent()).isFalse();
            assertThat(material.side()).isEqualTo(MaterialSide.FRONT);
            assertThat(material.depthTestEnabled()).isTrue();
            assertThat(material.depthWriteEnabled()).isTrue();
            assertThat(material.version()).isZero();
        }
    }

    @Test
    void versionsOnlyActualMaterialChanges() {
        try (BasicMaterial material = new BasicMaterial(Color.RED)) {
            material.setColor(Color.BLUE);
            material.setUsesVertexColors(true);
            material.setVisible(false);
            material.setOpacity(0.5f);
            material.setTransparent(true);
            material.setSide(MaterialSide.DOUBLE);
            material.setDepthTestEnabled(false);
            material.setDepthWriteEnabled(false);

            assertThat(material.version()).isEqualTo(8L);

            material.setColor(Color.BLUE);
            material.setUsesVertexColors(true);
            material.setVisible(false);
            material.setOpacity(0.5f);
            material.setTransparent(true);
            material.setSide(MaterialSide.DOUBLE);
            material.setDepthTestEnabled(false);
            material.setDepthWriteEnabled(false);

            assertThat(material.version()).isEqualTo(8L);
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidMaterialValues() {
        try (BasicMaterial material = new BasicMaterial()) {
            assertThatIllegalArgumentException().isThrownBy(() -> material.setOpacity(Float.NaN));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setOpacity(-0.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setOpacity(1.1f));
            assertThatNullPointerException().isThrownBy(() -> material.setColor(null));
            assertThatNullPointerException().isThrownBy(() -> material.setSide(null));
        }
        assertThatNullPointerException().isThrownBy(() -> new BasicMaterial(null));
    }

    @Test
    void closesTerminallyAndIdempotently() {
        BasicMaterial material = new BasicMaterial();
        material.close();
        material.close();

        assertThat(material.isClosed()).isTrue();
        assertThatIllegalStateException().isThrownBy(material::color);
        assertThatIllegalStateException().isThrownBy(material::version);
        assertThatIllegalStateException().isThrownBy(() -> material.setVisible(false));
    }
}
