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
import java.util.List;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

final class StandardMaterialTest {
    @Test
    void providesDocumentedDefaults() {
        try (StandardMaterial material = new StandardMaterial()) {
            assertThat(material.color()).isEqualTo(Color.WHITE);
            assertThat(material.metalness()).isZero();
            assertThat(material.roughness()).isOne();
            assertThat(material.emissive()).isEqualTo(Color.BLACK);
            assertThat(material.emissiveIntensity()).isOne();
            assertThat(material.occlusionStrength()).isOne();
            assertThat(material.normalScale(new Vector2f())).isEqualTo(new Vector2f(1.0f));
            assertThat(material.usesVertexColors()).isFalse();
            assertThat(material.colorMap()).isEmpty();
            assertThat(material.metalnessRoughnessMap()).isEmpty();
            assertThat(material.normalMap()).isEmpty();
            assertThat(material.occlusionMap()).isEmpty();
            assertThat(material.emissiveMap()).isEmpty();
            assertThat(material.version()).isZero();
        }
    }

    @Test
    void versionsOnlyActualScalarAndColorChanges() {
        try (StandardMaterial material = new StandardMaterial()) {
            material.setColor(Color.RED);
            material.setMetalness(0.8f);
            material.setRoughness(0.25f);
            material.setEmissive(Color.BLUE);
            material.setEmissiveIntensity(2.0f);
            material.setOcclusionStrength(0.5f);
            material.setNormalScale(0.75f, -0.5f);
            material.setUsesVertexColors(true);
            assertThat(material.version()).isEqualTo(8L);

            material.setColor(Color.RED);
            material.setMetalness(0.8f);
            material.setRoughness(0.25f);
            material.setEmissive(Color.BLUE);
            material.setEmissiveIntensity(2.0f);
            material.setOcclusionStrength(0.5f);
            material.setNormalScale(new Vector2f(0.75f, -0.5f));
            material.setUsesVertexColors(true);
            assertThat(material.version()).isEqualTo(8L);
        }
    }

    @Test
    void retainsAndClearsEverySharedTextureRole() {
        List<Texture> textures = List.of(
                Texture.baseColor(1, 1, new byte[4]),
                Texture.data(1, 1, new byte[4]),
                Texture.data(1, 1, new byte[4]),
                Texture.data(1, 1, new byte[4]),
                Texture.baseColor(1, 1, new byte[4]));
        try (StandardMaterial material = new StandardMaterial()) {
            material.setColorMap(textures.get(0));
            material.setMetalnessRoughnessMap(textures.get(1));
            material.setNormalMap(textures.get(2));
            material.setOcclusionMap(textures.get(3));
            material.setEmissiveMap(textures.get(4));

            assertThat(material.colorMap()).containsSame(textures.get(0));
            assertThat(material.metalnessRoughnessMap()).containsSame(textures.get(1));
            assertThat(material.normalMap()).containsSame(textures.get(2));
            assertThat(material.occlusionMap()).containsSame(textures.get(3));
            assertThat(material.emissiveMap()).containsSame(textures.get(4));
            assertThat(material.version()).isEqualTo(5L);

            material.clearColorMap();
            material.clearMetalnessRoughnessMap();
            material.clearNormalMap();
            material.clearOcclusionMap();
            material.clearEmissiveMap();
            assertThat(material.version()).isEqualTo(10L);
        } finally {
            textures.forEach(Texture::close);
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidPropertiesAndClosedTextures() {
        assertThatNullPointerException().isThrownBy(() -> new StandardMaterial(null));
        try (StandardMaterial material = new StandardMaterial()) {
            assertThatNullPointerException().isThrownBy(() -> material.setColor(null));
            assertThatNullPointerException().isThrownBy(() -> material.setEmissive(null));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setMetalness(-0.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setRoughness(1.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setEmissiveIntensity(Float.NaN));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setOcclusionStrength(-0.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setNormalScale(Float.NaN, 1.0f));
            assertThatNullPointerException().isThrownBy(() -> material.setNormalScale(null));

            Texture closedTexture = Texture.data(1, 1, new byte[4]);
            closedTexture.close();
            assertThatIllegalArgumentException().isThrownBy(() -> material.setColorMap(closedTexture));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setMetalnessRoughnessMap(closedTexture));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setNormalMap(closedTexture));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setOcclusionMap(closedTexture));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setEmissiveMap(closedTexture));
        }
    }

    @Test
    void closesTerminallyWithoutClosingSharedTextures() {
        Texture texture = Texture.baseColor(1, 1, new byte[4]);
        StandardMaterial material = new StandardMaterial();
        material.setColorMap(texture);

        material.close();

        assertThat(texture.isClosed()).isFalse();
        assertThatIllegalStateException().isThrownBy(material::color);
        assertThatIllegalStateException().isThrownBy(material::metalness);
        assertThatIllegalStateException().isThrownBy(material::roughness);
        assertThatIllegalStateException().isThrownBy(material::emissive);
        assertThatIllegalStateException().isThrownBy(material::colorMap);
        texture.close();
    }
}
