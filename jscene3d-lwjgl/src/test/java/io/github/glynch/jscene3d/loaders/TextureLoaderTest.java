/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.glynch.jscene3d.core.Texture;
import io.github.glynch.jscene3d.core.TextureColorSpace;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

final class TextureLoaderTest {
    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZlC8AAAAASUVORK5CYII=";

    @Test
    void loadsPngIntoCoreOwnedRgbaPixels(@TempDir Path temporaryDirectory) throws IOException {
        Path source = temporaryDirectory.resolve("pixel.png");
        Files.write(source, Base64.getDecoder().decode(ONE_PIXEL_PNG));

        try (Texture texture = TextureLoader.load(source)) {
            ByteBuffer pixels = ByteBuffer.allocate(texture.pixelByteCount());
            texture.copyPixelsTo(pixels);

            assertThat(texture.width()).isEqualTo(1);
            assertThat(texture.height()).isEqualTo(1);
            assertThat(texture.colorSpace()).isEqualTo(TextureColorSpace.SRGB);
            assertThat(pixels.position()).isEqualTo(4);
        }
    }

    @Test
    void loadsJpegIntoCoreOwnedRgbaPixels(@TempDir Path temporaryDirectory) {
        Path source = temporaryDirectory.resolve("pixel.jpg");
        ByteBuffer sourcePixels = MemoryUtil.memAlloc(3);
        try {
            sourcePixels.put((byte) 0xff).put((byte) 0x40).put((byte) 0x20).flip();
            assertThat(STBImageWrite.stbi_write_jpg(source.toString(), 1, 1, 3, sourcePixels, 90))
                    .isTrue();
        } finally {
            MemoryUtil.memFree(sourcePixels);
        }

        try (Texture texture = TextureLoader.load(source)) {
            assertThat(texture.width()).isEqualTo(1);
            assertThat(texture.height()).isEqualTo(1);
            assertThat(texture.pixelByteCount()).isEqualTo(4);
        }
    }

    @Test
    void rejectsUnsupportedFormatsBeforeStbDecoding(@TempDir Path temporaryDirectory) throws IOException {
        Path source = temporaryDirectory.resolve("pixel.gif");
        Files.write(source, new byte[] {'G', 'I', 'F', '8', '9', 'a'});

        assertThatExceptionOfType(TextureLoadException.class)
                .isThrownBy(() -> TextureLoader.load(source))
                .withMessageContaining(source.toString())
                .withMessageContaining("PNG or JPEG")
                .extracting(TextureLoadException::source)
                .isEqualTo(source);
    }

    @Test
    void reportsStbDiagnosticsForInvalidSupportedImages(@TempDir Path temporaryDirectory) throws IOException {
        Path source = temporaryDirectory.resolve("broken.png");
        Files.write(source, Base64.getDecoder().decode(ONE_PIXEL_PNG.substring(0, 32)));

        assertThatExceptionOfType(TextureLoadException.class)
                .isThrownBy(() -> TextureLoader.load(source))
                .withMessageContaining(source.toString())
                .withMessageContaining("Cannot decode");
    }

    @Test
    void reportsIoFailuresWithTheirSource(@TempDir Path temporaryDirectory) {
        Path source = temporaryDirectory.resolve("missing.png");

        assertThatExceptionOfType(TextureLoadException.class)
                .isThrownBy(() -> TextureLoader.load(source))
                .withMessageContaining(source.toString())
                .withCauseInstanceOf(IOException.class);
    }
}
