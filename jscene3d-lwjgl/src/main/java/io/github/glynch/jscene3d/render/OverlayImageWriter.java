/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.stb.STBImageWrite.stbi_write_png;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Objects;

/** Writes immutable overlay images to portable PNG files. */
public final class OverlayImageWriter {
    /** Prevents instantiation of this stateless writer. */
    private OverlayImageWriter() {
        throw new AssertionError("OverlayImageWriter cannot be instantiated");
    }

    /**
     * Writes a full-colour overlay image as an RGBA PNG.
     *
     * @param destination destination PNG path
     * @param image full-colour image to write
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the supplied image is an alpha mask
     * @throws IllegalStateException if the native encoder cannot write the file
     */
    public static void writePng(Path destination, OverlayImage image) {
        Path validDestination = Objects.requireNonNull(destination, "destination");
        OverlayImage validImage = Objects.requireNonNull(image, "image");
        if (validImage.format() != OverlayImageFormat.SRGB_RGBA) {
            throw new IllegalArgumentException("Only full-colour overlay images can be written as RGBA PNG files");
        }
        byte[] pixels = validImage.pixels();
        ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length);
        buffer.put(pixels).flip();
        boolean written = stbi_write_png(
                validDestination.toString(),
                validImage.width(),
                validImage.height(),
                4,
                buffer,
                validImage.width() * 4);
        if (!written) {
            throw new IllegalStateException("Could not write PNG image: " + validDestination);
        }
    }
}
