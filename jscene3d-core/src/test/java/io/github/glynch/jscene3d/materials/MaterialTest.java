/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.textures.Texture;
import org.junit.jupiter.api.Test;

final class MaterialTest {
    @Test
    void providesDocumentedBasicMaterialDefaults() {
        try (BasicMaterial material = new BasicMaterial()) {
            assertThat(material.color()).isEqualTo(Color.WHITE);
            assertThat(material.usesVertexColors()).isFalse();
            assertThat(material.colorMap()).isEmpty();
            assertThat(material.visible()).isTrue();
            assertThat(material.opacity()).isEqualTo(1.0f);
            assertThat(material.transparent()).isFalse();
            assertThat(material.alphaMode()).isEqualTo(AlphaMode.OPAQUE);
            assertThat(material.alphaCutoff()).isEqualTo(0.5f);
            assertThat(material.side()).isEqualTo(MaterialSide.FRONT);
            assertThat(material.depthTestEnabled()).isTrue();
            assertThat(material.depthWriteEnabled()).isTrue();
            assertThat(material.depthFunction()).isEqualTo(DepthFunction.LESS_OR_EQUAL);
            assertThat(material.version()).isZero();
        }
    }

    @Test
    void versionsOnlyActualMaterialChanges() {
        try (BasicMaterial material = new BasicMaterial(Color.RED);
                Texture texture = Texture.baseColor(1, 1, new byte[4])) {
            material.setColor(Color.BLUE);
            material.setUsesVertexColors(true);
            material.setVisible(false);
            material.setOpacity(0.5f);
            material.setTransparent(true);
            material.setAlphaCutoff(0.25f);
            material.setSide(MaterialSide.DOUBLE);
            material.setDepthTestEnabled(false);
            material.setDepthWriteEnabled(false);
            material.setDepthFunction(DepthFunction.GREATER);
            material.setColorMap(texture);

            assertThat(material.version()).isEqualTo(11L);

            material.setColor(Color.BLUE);
            material.setUsesVertexColors(true);
            material.setVisible(false);
            material.setOpacity(0.5f);
            material.setTransparent(true);
            material.setAlphaCutoff(0.25f);
            material.setSide(MaterialSide.DOUBLE);
            material.setDepthTestEnabled(false);
            material.setDepthWriteEnabled(false);
            material.setDepthFunction(DepthFunction.GREATER);
            material.setColorMap(texture);

            assertThat(material.version()).isEqualTo(11L);
            assertThat(material.colorMap()).containsSame(texture);

            material.clearColorMap();
            material.clearColorMap();
            assertThat(material.version()).isEqualTo(12L);
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidMaterialValues() {
        try (BasicMaterial material = new BasicMaterial()) {
            assertThatIllegalArgumentException().isThrownBy(() -> material.setOpacity(Float.NaN));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setOpacity(-0.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setOpacity(1.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setAlphaCutoff(Float.NaN));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setAlphaCutoff(-0.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setAlphaCutoff(1.1f));
            assertThatNullPointerException().isThrownBy(() -> material.setAlphaMode(null));
            assertThatNullPointerException().isThrownBy(() -> material.setColor(null));
            assertThatNullPointerException().isThrownBy(() -> material.setSide(null));
            assertThatNullPointerException().isThrownBy(() -> material.setDepthFunction(null));
            assertThatNullPointerException().isThrownBy(() -> material.setColorMap(null));
        }
        assertThatNullPointerException().isThrownBy(() -> new BasicMaterial(null));
    }

    @Test
    void exposesBooleanTransparencyAsAnAlphaModeAlias() {
        try (BasicMaterial material = new BasicMaterial()) {
            material.setAlphaMode(AlphaMode.MASK);
            assertThat(material.transparent()).isFalse();

            material.setTransparent(true);
            assertThat(material.alphaMode()).isEqualTo(AlphaMode.BLEND);

            material.setTransparent(false);
            assertThat(material.alphaMode()).isEqualTo(AlphaMode.OPAQUE);
        }
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

    @Test
    void rejectsAClosedColorMapWithoutOwningAnOpenOne() {
        Texture closedTexture = Texture.baseColor(1, 1, new byte[4]);
        closedTexture.close();
        try (BasicMaterial material = new BasicMaterial()) {
            assertThatIllegalArgumentException().isThrownBy(() -> material.setColorMap(closedTexture));
        }

        Texture sharedTexture = Texture.baseColor(1, 1, new byte[4]);
        BasicMaterial material = new BasicMaterial();
        material.setColorMap(sharedTexture);
        material.close();
        assertThat(sharedTexture.isClosed()).isFalse();
        sharedTexture.close();
    }
}
