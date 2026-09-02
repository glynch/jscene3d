/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class TextureRegionTest {
    @Test
    void representsTheCompleteTextureWithASharedRegion() {
        assertThat(TextureRegion.full()).isSameAs(TextureRegion.full());
        assertThat(TextureRegion.full()).isEqualTo(new TextureRegion(0.0f, 0.0f, 1.0f, 1.0f));
    }

    @Test
    void convertsTopRowFirstPixelCoordinatesToNormalizedCoordinates() {
        TextureRegion region = TextureRegion.fromPixels(32, 16, 32, 16, 128, 64);

        assertThat(region).isEqualTo(new TextureRegion(0.25f, 0.5f, 0.25f, 0.25f));
    }

    @Test
    void rejectsInvalidNormalizedRegions() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TextureRegion(-0.1f, 0.0f, 0.5f, 0.5f));
        assertThatIllegalArgumentException().isThrownBy(() -> new TextureRegion(0.0f, 0.0f, 0.0f, 0.5f));
        assertThatIllegalArgumentException().isThrownBy(() -> new TextureRegion(0.8f, 0.0f, 0.3f, 0.5f));
    }

    @Test
    void rejectsInvalidPixelRegions() {
        assertThatIllegalArgumentException().isThrownBy(() -> TextureRegion.fromPixels(-1, 0, 1, 1, 4, 4));
        assertThatIllegalArgumentException().isThrownBy(() -> TextureRegion.fromPixels(0, 0, 5, 1, 4, 4));
        assertThatIllegalArgumentException().isThrownBy(() -> TextureRegion.fromPixels(0, 0, 1, 1, 0, 4));
    }
}
