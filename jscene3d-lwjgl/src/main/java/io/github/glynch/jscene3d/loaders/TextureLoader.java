/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import io.github.glynch.jscene3d.core.Texture;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

/**
 * Loads officially supported disk images into renderer-independent texture descriptions.
 *
 * <p>Loading is synchronous on the calling thread; this component creates no background threads.
 */
public final class TextureLoader {
    private static final int RGBA_CHANNELS = 4;

    /** Prevents instantiation of this stateless loader. */
    private TextureLoader() {
        throw new AssertionError("TextureLoader cannot be instantiated");
    }

    /**
     * Loads a PNG or JPEG as an sRGB base-color texture.
     *
     * <p>STB decoding is completed and its native memory is freed before this method returns. The
     * returned texture owns one Java copy in top-row-first RGBA8 order.
     *
     * @param source PNG or JPEG path
     * @return new application-owned base-color texture
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws TextureLoadException if the file cannot be read, has an unsupported signature, or
     *     cannot be decoded
     */
    public static Texture load(Path source) {
        Path validSource = Objects.requireNonNull(source, "source");
        byte[] encoded = read(validSource);
        requireSupportedFormat(validSource, encoded);

        ByteBuffer encodedBuffer = MemoryUtil.memAlloc(encoded.length);
        try {
            encodedBuffer.put(encoded).flip();
            int[] width = new int[1];
            int[] height = new int[1];
            int[] sourceChannels = new int[1];
            @Nullable
            ByteBuffer decoded =
                    STBImage.stbi_load_from_memory(encodedBuffer, width, height, sourceChannels, RGBA_CHANNELS);
            if (decoded == null) {
                @Nullable String reason = STBImage.stbi_failure_reason();
                String diagnostic = reason == null ? "unknown STB decoding failure" : reason;
                throw new TextureLoadException(
                        validSource, "Cannot decode texture image " + validSource + ": " + diagnostic);
            }
            try {
                return Texture.baseColor(width[0], height[0], decoded);
            } finally {
                STBImage.stbi_image_free(decoded);
            }
        } finally {
            MemoryUtil.memFree(encodedBuffer);
        }
    }

    /** Reads the complete encoded image or translates its I/O failure. */
    private static byte[] read(Path source) {
        try {
            return Files.readAllBytes(source);
        } catch (IOException exception) {
            throw new TextureLoadException(source, "Cannot read texture image " + source, exception);
        }
    }

    /** Restricts the version 0.1 compatibility promise to PNG and JPEG signatures. */
    private static void requireSupportedFormat(Path source, byte[] encoded) {
        boolean png = encoded.length >= 8
                && Byte.toUnsignedInt(encoded[0]) == 0x89
                && encoded[1] == 'P'
                && encoded[2] == 'N'
                && encoded[3] == 'G'
                && encoded[4] == 0x0d
                && encoded[5] == 0x0a
                && encoded[6] == 0x1a
                && encoded[7] == 0x0a;
        boolean jpeg = encoded.length >= 3
                && Byte.toUnsignedInt(encoded[0]) == 0xff
                && Byte.toUnsignedInt(encoded[1]) == 0xd8
                && Byte.toUnsignedInt(encoded[2]) == 0xff;
        if (!png && !jpeg) {
            throw new TextureLoadException(
                    source, "Unsupported texture image format for " + source + "; expected PNG or JPEG");
        }
    }
}
