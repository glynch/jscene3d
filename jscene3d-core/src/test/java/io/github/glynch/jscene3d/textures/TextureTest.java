/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

final class TextureTest {
    @Test
    void providesBaseColorDefaultsAndDefensivelyCopiesPixels() {
        byte[] pixels = rgba2x1();
        try (Texture texture = Texture.baseColor(2, 1, pixels)) {
            pixels[0] = 0;
            ByteBuffer copy = ByteBuffer.allocate(8);
            texture.copyPixelsTo(copy);

            assertThat(texture.width()).isEqualTo(2);
            assertThat(texture.height()).isEqualTo(1);
            assertThat(texture.pixelFormat()).isEqualTo(TexturePixelFormat.RGBA8);
            assertThat(texture.pixelByteCount()).isEqualTo(8);
            assertThat(texture.colorSpace()).isEqualTo(TextureColorSpace.SRGB);
            assertThat(texture.minificationFilter()).isEqualTo(TextureFilter.LINEAR_MIPMAP_LINEAR);
            assertThat(texture.magnificationFilter()).isEqualTo(TextureFilter.LINEAR);
            assertThat(texture.horizontalWrap()).isEqualTo(TextureWrap.CLAMP_TO_EDGE);
            assertThat(texture.verticalWrap()).isEqualTo(TextureWrap.CLAMP_TO_EDGE);
            assertThat(texture.mipmapMode()).isEqualTo(MipmapMode.GENERATE);
            assertThat(copy.array()).containsExactly((byte) 0xff, 0, 0, (byte) 0xff, 0, (byte) 0xff, 0, (byte) 0xff);
        }
    }

    @Test
    void providesLinearDataTexture() {
        try (Texture texture = Texture.data(2, 1, rgba2x1())) {
            assertThat(texture.colorSpace()).isEqualTo(TextureColorSpace.LINEAR);
        }
    }

    @Test
    void acceptsBufferPixelsWithoutChangingOrRetainingTheBuffer() {
        ByteBuffer pixels = ByteBuffer.allocate(10);
        pixels.position(2);
        pixels.put(rgba2x1()).flip().position(2);
        try (Texture texture = Texture.baseColor(2, 1, pixels)) {
            pixels.put(2, (byte) 0);
            ByteBuffer copy = ByteBuffer.allocate(8);
            texture.copyPixelsTo(copy);

            assertThat(pixels.position()).isEqualTo(2);
            assertThat(copy.array()).containsExactly((byte) 0xff, 0, 0, (byte) 0xff, 0, (byte) 0xff, 0, (byte) 0xff);
        }
    }

    @Test
    void versionsImageAndSamplerChangesIndependently() {
        try (Texture texture = Texture.baseColor(2, 1, rgba2x1())) {
            texture.setColorSpace(TextureColorSpace.LINEAR);
            texture.setImage(1, 1, new byte[] {1, 2, 3, 4});
            texture.setHorizontalWrap(TextureWrap.REPEAT);
            texture.setVerticalWrap(TextureWrap.MIRRORED_REPEAT);
            texture.setMagnificationFilter(TextureFilter.NEAREST);
            texture.setMinificationFilter(TextureFilter.LINEAR);
            texture.setMipmapMode(MipmapMode.NONE);

            assertThat(texture.version()).isEqualTo(7L);
            assertThat(texture.imageVersion()).isEqualTo(2L);
            assertThat(texture.samplerVersion()).isEqualTo(5L);

            texture.setColorSpace(TextureColorSpace.LINEAR);
            texture.setHorizontalWrap(TextureWrap.REPEAT);
            texture.setMipmapMode(MipmapMode.NONE);

            assertThat(texture.version()).isEqualTo(7L);
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidImagesAndSamplerCombinations() {
        assertThatIllegalArgumentException().isThrownBy(() -> Texture.baseColor(0, 1, new byte[0]));
        assertThatIllegalArgumentException().isThrownBy(() -> Texture.baseColor(1, 1, new byte[3]));
        assertThatNullPointerException().isThrownBy(() -> Texture.baseColor(1, 1, (byte[]) null));

        try (Texture texture = Texture.baseColor(1, 1, new byte[4])) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> texture.setMagnificationFilter(TextureFilter.LINEAR_MIPMAP_LINEAR));
            assertThatIllegalArgumentException().isThrownBy(() -> texture.setMipmapMode(MipmapMode.NONE));
            texture.setMinificationFilter(TextureFilter.LINEAR);
            texture.setMipmapMode(MipmapMode.NONE);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> texture.setMinificationFilter(TextureFilter.NEAREST_MIPMAP_NEAREST));
            ByteBuffer tooSmall = ByteBuffer.allocate(3);
            assertThatIllegalArgumentException().isThrownBy(() -> texture.copyPixelsTo(tooSmall));
        }
    }

    @Test
    void closesTerminallyAndIdempotently() {
        Texture texture = Texture.baseColor(1, 1, new byte[4]);
        texture.close();
        texture.close();

        assertThat(texture.isClosed()).isTrue();
        assertThatIllegalStateException().isThrownBy(texture::width);
        assertThatIllegalStateException().isThrownBy(texture::version);
        assertThatIllegalStateException().isThrownBy(() -> texture.setHorizontalWrap(TextureWrap.REPEAT));
    }

    private static byte[] rgba2x1() {
        return new byte[] {(byte) 0xff, 0, 0, (byte) 0xff, 0, (byte) 0xff, 0, (byte) 0xff};
    }
}
