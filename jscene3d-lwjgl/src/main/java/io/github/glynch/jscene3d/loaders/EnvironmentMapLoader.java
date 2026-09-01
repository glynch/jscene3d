/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import io.github.glynch.jscene3d.textures.EnvironmentMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

/** Loads Radiance HDR images into renderer-independent equirectangular environment maps. */
public final class EnvironmentMapLoader {
    private static final int RGB_CHANNELS = 3;

    /** Prevents instantiation of this stateless loader. */
    private EnvironmentMapLoader() {
        throw new AssertionError("EnvironmentMapLoader cannot be instantiated");
    }

    /**
     * Loads a Radiance HDR image synchronously.
     *
     * <p>STB native decoding storage is released before this method returns. The returned map owns
     * one Java RGB floating-point copy and must be closed by the application.
     *
     * @param source Radiance {@code .hdr} path
     * @return new application-owned equirectangular environment map
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws EnvironmentMapLoadException if the file cannot be read or decoded as Radiance HDR
     */
    public static EnvironmentMap load(Path source) {
        Path validSource = Objects.requireNonNull(source, "source");
        byte[] encoded = read(validSource);
        ByteBuffer encodedBuffer = MemoryUtil.memAlloc(encoded.length);
        try {
            encodedBuffer.put(encoded).flip();
            if (!STBImage.stbi_is_hdr_from_memory(encodedBuffer)) {
                throw new EnvironmentMapLoadException(
                        validSource, "Unsupported environment image format for " + validSource + "; expected HDR");
            }
            encodedBuffer.rewind();
            int[] width = new int[1];
            int[] height = new int[1];
            int[] sourceChannels = new int[1];
            @Nullable
            FloatBuffer decoded =
                    STBImage.stbi_loadf_from_memory(encodedBuffer, width, height, sourceChannels, RGB_CHANNELS);
            if (decoded == null) {
                @Nullable String reason = STBImage.stbi_failure_reason();
                String diagnostic = reason == null ? "unknown STB decoding failure" : reason;
                throw new EnvironmentMapLoadException(
                        validSource, "Cannot decode environment image " + validSource + ": " + diagnostic);
            }
            try {
                return EnvironmentMap.equirectangular(width[0], height[0], decoded);
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
            throw new EnvironmentMapLoadException(source, "Cannot read environment image " + source, exception);
        }
    }
}
