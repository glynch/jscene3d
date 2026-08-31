/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.core.MipmapMode;
import io.github.glynch.jscene3d.core.Texture;
import io.github.glynch.jscene3d.core.TextureColorSpace;
import io.github.glynch.jscene3d.core.TextureFilter;
import io.github.glynch.jscene3d.core.TextureWrap;
import io.github.glynch.jscene3d.platform.Window;
import org.junit.jupiter.api.Test;

final class TextureResourceIT {
    @Test
    void synchronizesImageSamplerMipmapAndStagingChanges() {
        RenderStatistics statistics = new RenderStatistics();

        try (Window ignored = Window.create("Texture resource synchronization test");
                Texture texture = Texture.data(1, 1, opaqueWhitePixel());
                TextureResource resource = new TextureResource()) {
            resource.synchronize(texture, statistics);
            assertThat(statistics.textureUploads()).isOne();
            assertThat(statistics.textureUploadBytes()).isEqualTo(4L);

            statistics.beginFrame();
            resource.synchronize(texture, statistics);
            assertThat(statistics.textureUploads()).isZero();

            for (TextureFilter filter : TextureFilter.values()) {
                texture.setMinificationFilter(filter);
                resource.synchronize(texture, statistics);
            }
            texture.setMagnificationFilter(TextureFilter.NEAREST);
            resource.synchronize(texture, statistics);
            texture.setMagnificationFilter(TextureFilter.LINEAR);
            resource.synchronize(texture, statistics);

            for (TextureWrap wrap : TextureWrap.values()) {
                texture.setHorizontalWrap(wrap);
                texture.setVerticalWrap(wrap);
                resource.synchronize(texture, statistics);
            }

            texture.setColorSpace(TextureColorSpace.SRGB);
            resource.synchronize(texture, statistics);
            texture.setImage(2, 1, twoOpaquePixels());
            resource.synchronize(texture, statistics);

            texture.setMinificationFilter(TextureFilter.LINEAR);
            texture.setMipmapMode(MipmapMode.NONE);
            resource.synchronize(texture, statistics);

            assertThat(statistics.textureUploads()).isEqualTo(2);
            assertThat(statistics.textureUploadBytes()).isEqualTo(12L);
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
