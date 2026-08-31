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

final class LambertMaterialTest {
    @Test
    void providesDocumentedDefaults() {
        try (LambertMaterial material = new LambertMaterial()) {
            assertThat(material.color()).isEqualTo(Color.WHITE);
            assertThat(material.usesVertexColors()).isFalse();
            assertThat(material.colorMap()).isEmpty();
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
    void acceptsAnInitialBaseColor() {
        try (LambertMaterial material = new LambertMaterial(Color.BLUE)) {
            assertThat(material.color()).isEqualTo(Color.BLUE);
        }
    }

    @Test
    void versionsOnlyActualLambertPropertyChanges() {
        try (LambertMaterial material = new LambertMaterial();
                Texture texture = Texture.baseColor(1, 1, new byte[4])) {
            material.setColor(Color.RED);
            material.setUsesVertexColors(true);
            material.setColorMap(texture);

            assertThat(material.version()).isEqualTo(3L);
            assertThat(material.colorMap()).containsSame(texture);

            material.setColor(Color.RED);
            material.setUsesVertexColors(true);
            material.setColorMap(texture);
            assertThat(material.version()).isEqualTo(3L);

            material.clearColorMap();
            material.clearColorMap();
            assertThat(material.version()).isEqualTo(4L);
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidLambertProperties() {
        assertThatNullPointerException().isThrownBy(() -> new LambertMaterial(null));
        try (LambertMaterial material = new LambertMaterial()) {
            assertThatNullPointerException().isThrownBy(() -> material.setColor(null));
            assertThatNullPointerException().isThrownBy(() -> material.setColorMap(null));

            Texture closedTexture = Texture.baseColor(1, 1, new byte[4]);
            closedTexture.close();
            assertThatIllegalArgumentException().isThrownBy(() -> material.setColorMap(closedTexture));
        }
    }

    @Test
    void closesTerminallyWithoutClosingSharedTextures() {
        Texture texture = Texture.baseColor(1, 1, new byte[4]);
        LambertMaterial material = new LambertMaterial();
        material.setColorMap(texture);

        material.close();
        material.close();

        assertThat(material.isClosed()).isTrue();
        assertThat(texture.isClosed()).isFalse();
        assertThatIllegalStateException().isThrownBy(material::color);
        assertThatIllegalStateException().isThrownBy(material::usesVertexColors);
        assertThatIllegalStateException().isThrownBy(material::colorMap);
        assertThatIllegalStateException().isThrownBy(() -> material.setColor(Color.RED));
        assertThatIllegalStateException().isThrownBy(() -> material.setUsesVertexColors(true));
        assertThatIllegalStateException().isThrownBy(material::clearColorMap);
        texture.close();
    }
}
