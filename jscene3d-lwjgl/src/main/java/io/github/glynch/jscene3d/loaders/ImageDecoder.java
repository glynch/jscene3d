/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

/** Shared synchronous PNG and JPEG decoder for public image-loading entry points. */
final class ImageDecoder {
    private static final int RGBA_CHANNELS = 4;

    /** Prevents instantiation of this stateless decoder. */
    private ImageDecoder() {
        throw new AssertionError("ImageDecoder cannot be instantiated");
    }

    /** Decodes one supported image and transfers its temporary RGBA buffer to a result factory. */
    static <T> T decode(Path source, DecodedImageFactory<T> factory) {
        Path validSource = Objects.requireNonNull(source, "source");
        DecodedImageFactory<T> validFactory = Objects.requireNonNull(factory, "factory");
        byte[] encoded = read(validSource);
        return decode(validSource, encoded, validFactory);
    }

    /** Decodes supplied image bytes and transfers its temporary RGBA buffer to a result factory. */
    static <T> T decode(Path source, byte[] encoded, DecodedImageFactory<T> factory) {
        Path validSource = Objects.requireNonNull(source, "source");
        byte[] validEncoded = Objects.requireNonNull(encoded, "encoded");
        DecodedImageFactory<T> validFactory = Objects.requireNonNull(factory, "factory");
        requireSupportedFormat(validSource, validEncoded);

        ByteBuffer encodedBuffer = MemoryUtil.memAlloc(validEncoded.length);
        try {
            encodedBuffer.put(validEncoded).flip();
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
                return validFactory.create(width[0], height[0], decoded);
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

    /** Restricts the compatibility promise to PNG and JPEG signatures. */
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

    /** Converts a temporary tightly packed RGBA8 decoding into an owned result. */
    @FunctionalInterface
    interface DecodedImageFactory<T> {
        /** Creates a result before the decoder releases the supplied native buffer. */
        T create(int width, int height, ByteBuffer pixels);
    }
}
