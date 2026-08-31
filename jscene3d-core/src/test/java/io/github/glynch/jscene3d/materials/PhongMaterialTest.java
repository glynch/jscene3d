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

final class PhongMaterialTest {
    @Test
    void providesDocumentedDefaults() {
        try (PhongMaterial material = new PhongMaterial()) {
            assertThat(material.color()).isEqualTo(Color.WHITE);
            assertThat(material.emissive()).isEqualTo(Color.BLACK);
            assertThat(material.emissiveIntensity()).isEqualTo(1.0f);
            assertThat(material.specular()).isEqualTo(Color.srgb(0x111111));
            assertThat(material.shininess()).isEqualTo(30.0f);
            assertThat(material.usesVertexColors()).isFalse();
            assertThat(material.colorMap()).isEmpty();
            assertThat(material.version()).isZero();
        }
    }

    @Test
    void acceptsAnInitialBaseColor() {
        try (PhongMaterial material = new PhongMaterial(Color.BLUE)) {
            assertThat(material.color()).isEqualTo(Color.BLUE);
        }
    }

    @Test
    void versionsOnlyActualPhongPropertyChanges() {
        try (PhongMaterial material = new PhongMaterial();
                Texture texture = Texture.baseColor(1, 1, new byte[4])) {
            material.setColor(Color.RED);
            material.setEmissive(Color.BLUE);
            material.setEmissiveIntensity(2.0f);
            material.setSpecular(Color.WHITE);
            material.setShininess(64.0f);
            material.setUsesVertexColors(true);
            material.setColorMap(texture);

            assertThat(material.version()).isEqualTo(7L);
            assertThat(material.colorMap()).containsSame(texture);

            material.setColor(Color.RED);
            material.setEmissive(Color.BLUE);
            material.setEmissiveIntensity(2.0f);
            material.setSpecular(Color.WHITE);
            material.setShininess(64.0f);
            material.setUsesVertexColors(true);
            material.setColorMap(texture);
            assertThat(material.version()).isEqualTo(7L);

            material.clearColorMap();
            material.clearColorMap();
            assertThat(material.version()).isEqualTo(8L);
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidPhongProperties() {
        assertThatNullPointerException().isThrownBy(() -> new PhongMaterial(null));
        try (PhongMaterial material = new PhongMaterial()) {
            assertThatNullPointerException().isThrownBy(() -> material.setColor(null));
            assertThatNullPointerException().isThrownBy(() -> material.setEmissive(null));
            assertThatNullPointerException().isThrownBy(() -> material.setSpecular(null));
            assertThatNullPointerException().isThrownBy(() -> material.setColorMap(null));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setEmissiveIntensity(-0.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setShininess(Float.NaN));

            Texture closedTexture = Texture.baseColor(1, 1, new byte[4]);
            closedTexture.close();
            assertThatIllegalArgumentException().isThrownBy(() -> material.setColorMap(closedTexture));
        }
    }

    @Test
    void closesTerminallyWithoutClosingSharedTextures() {
        Texture texture = Texture.baseColor(1, 1, new byte[4]);
        PhongMaterial material = new PhongMaterial();
        material.setColorMap(texture);

        material.close();

        assertThat(material.isClosed()).isTrue();
        assertThat(texture.isClosed()).isFalse();
        assertThatIllegalStateException().isThrownBy(material::color);
        assertThatIllegalStateException().isThrownBy(material::emissive);
        assertThatIllegalStateException().isThrownBy(material::specular);
        assertThatIllegalStateException().isThrownBy(material::shininess);
        assertThatIllegalStateException().isThrownBy(material::colorMap);
        texture.close();
    }
}
