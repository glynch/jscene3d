/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.textures.MipmapMode;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureColorSpace;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import org.junit.jupiter.api.Test;

final class TextureResourceIT {
    @Test
    void synchronizesImageSamplerMipmapAndStagingChanges() {
        try (Window ignored = Window.create("Texture resource synchronization test");
                Texture texture = Texture.data(1, 1, opaqueWhitePixel());
                TextureResource resource = new TextureResource()) {
            assertThat(resource.synchronize(texture)).isEqualTo(4L);

            assertThat(resource.synchronize(texture)).isZero();

            for (TextureFilter filter : TextureFilter.values()) {
                texture.setMinificationFilter(filter);
                assertThat(resource.synchronize(texture)).isZero();
            }
            texture.setMagnificationFilter(TextureFilter.NEAREST);
            assertThat(resource.synchronize(texture)).isZero();
            texture.setMagnificationFilter(TextureFilter.LINEAR);
            assertThat(resource.synchronize(texture)).isZero();

            for (TextureWrap wrap : TextureWrap.values()) {
                texture.setHorizontalWrap(wrap);
                texture.setVerticalWrap(wrap);
                assertThat(resource.synchronize(texture)).isZero();
            }

            texture.setColorSpace(TextureColorSpace.SRGB);
            assertThat(resource.synchronize(texture)).isEqualTo(4L);
            texture.setImage(2, 1, twoOpaquePixels());
            assertThat(resource.synchronize(texture)).isEqualTo(8L);

            texture.setMinificationFilter(TextureFilter.LINEAR);
            texture.setMipmapMode(MipmapMode.NONE);
            assertThat(resource.synchronize(texture)).isZero();
        }
    }

    /** Returns one opaque white RGBA8 pixel. */
    private static byte[] opaqueWhitePixel() {
        return new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    }

    /** Returns two opaque RGBA8 pixels. */
    private static byte[] twoOpaquePixels() {
        return new byte[] {(byte) 255, 0, 0, (byte) 255, 0, (byte) 255, 0, (byte) 255};
    }
}
