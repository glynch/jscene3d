/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf.internal;

import de.javagl.jgltf.model.ImageModel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import javax.imageio.ImageIO;

/** Decodes glTF PNG and JPEG image bytes into top-row-first RGBA8 pixels. */
public final class ImageDecoder {
    /** Prevents instantiation of this static decoder. */
    private ImageDecoder() {
        throw new AssertionError("ImageDecoder cannot be instantiated");
    }

    /**
     * Decodes one JglTF image model.
     *
     * @param imageModel image data to decode
     * @return decoded dimensions and pixels
     * @throws IOException if no registered ImageIO reader can decode the data
     */
    public static DecodedImage decode(ImageModel imageModel) throws IOException {
        ImageModel validImage = Objects.requireNonNull(imageModel, "imageModel");
        ByteBuffer source =
                Objects.requireNonNull(validImage.getImageData(), "image data").duplicate();
        byte[] encoded = new byte[source.remaining()];
        source.get(encoded);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(encoded));
        if (image == null) {
            throw new IOException("Unsupported or invalid glTF image data");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] pixels = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)];
        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                pixels[offset++] = (byte) (argb >>> 16);
                pixels[offset++] = (byte) (argb >>> 8);
                pixels[offset++] = (byte) argb;
                pixels[offset++] = (byte) (argb >>> 24);
            }
        }
        return new DecodedImage(width, height, pixels);
    }

    /** Decoded image payload retained only during conversion. */
    public static final class DecodedImage {
        private final int width;
        private final int height;
        private final byte[] pixels;

        /**
         * Creates a decoded image payload.
         *
         * @param width positive image width
         * @param height positive image height
         * @param pixels top-row-first RGBA8 pixels
         */
        public DecodedImage(int width, int height, byte[] pixels) {
            this.width = width;
            this.height = height;
            this.pixels = Objects.requireNonNull(pixels, "pixels");
        }

        /**
         * Returns the image width.
         *
         * @return positive image width
         */
        public int width() {
            return width;
        }

        /**
         * Returns the image height.
         *
         * @return positive image height
         */
        public int height() {
            return height;
        }

        /**
         * Returns the conversion-owned pixel array.
         *
         * @return top-row-first RGBA8 pixels
         */
        public byte[] pixels() {
            return pixels;
        }
    }
}
