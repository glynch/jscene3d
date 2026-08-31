/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.lwjgl.internal.Preconditions;
import java.util.Arrays;
import java.util.Objects;

/** Immutable single-channel image that a renderer can use as an overlay alpha mask. */
public final class OverlayImage {
    private final int width;
    private final int height;
    private final byte[] pixels;
    private final Region fullRegion;

    /** Retains validated dimensions and an owned pixel copy. */
    private OverlayImage(int width, int height, byte[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
        fullRegion = new Region(this, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    /**
     * Creates an immutable alpha mask from row-major unsigned bytes.
     *
     * <p>The input is defensively copied. A value of zero is transparent and {@code 255} is fully
     * opaque.
     *
     * @param width positive image width
     * @param height positive image height
     * @param pixels exactly {@code width * height} row-major alpha bytes
     * @return immutable overlay image
     * @throws NullPointerException if {@code pixels} is {@code null}
     * @throws IllegalArgumentException if a dimension or the array length is invalid
     */
    public static OverlayImage alphaMask(int width, int height, byte[] pixels) {
        int validWidth = Preconditions.requirePositive(width, "width");
        int validHeight = Preconditions.requirePositive(height, "height");
        byte[] validPixels = Objects.requireNonNull(pixels, "pixels");
        long expectedLength = (long) validWidth * validHeight;
        if (validPixels.length != expectedLength) {
            throw new IllegalArgumentException(
                    "pixels length must equal width * height: " + validPixels.length + " != " + expectedLength);
        }
        return new OverlayImage(validWidth, validHeight, Arrays.copyOf(validPixels, validPixels.length));
    }

    /**
     * Returns the image width.
     *
     * @return positive pixel width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the image height.
     *
     * @return positive pixel height
     */
    public int height() {
        return height;
    }

    /**
     * Returns a reusable region covering the complete image.
     *
     * @return complete normalized image region
     */
    public Region fullRegion() {
        return fullRegion;
    }

    /**
     * Creates a reusable normalized region of this image.
     *
     * @param minimumU horizontal origin in {@code [0, 1]}
     * @param minimumV vertical origin in {@code [0, 1]}
     * @param maximumU horizontal endpoint in {@code [0, 1]}
     * @param maximumV vertical endpoint in {@code [0, 1]}
     * @return immutable image region
     * @throws IllegalArgumentException if a coordinate is invalid or an interval is reversed
     */
    public Region region(float minimumU, float minimumV, float maximumU, float maximumV) {
        float validMinimumU = Preconditions.requireUnitInterval(minimumU, "minimumU");
        float validMinimumV = Preconditions.requireUnitInterval(minimumV, "minimumV");
        float validMaximumU = Preconditions.requireUnitInterval(maximumU, "maximumU");
        float validMaximumV = Preconditions.requireUnitInterval(maximumV, "maximumV");
        if (validMinimumU > validMaximumU || validMinimumV > validMaximumV) {
            throw new IllegalArgumentException("minimum texture coordinates must not exceed maximum coordinates");
        }
        return new Region(this, validMinimumU, validMinimumV, validMaximumU, validMaximumV);
    }

    /** Returns renderer-internal immutable pixel storage. */
    byte[] pixels() {
        return pixels;
    }

    /** Immutable normalized rectangular region of an {@link OverlayImage}. */
    public static final class Region {
        private final OverlayImage image;
        private final float minimumU;
        private final float minimumV;
        private final float maximumU;
        private final float maximumV;

        /** Retains one validated region. */
        private Region(OverlayImage image, float minimumU, float minimumV, float maximumU, float maximumV) {
            this.image = image;
            this.minimumU = minimumU;
            this.minimumV = minimumV;
            this.maximumU = maximumU;
            this.maximumV = maximumV;
        }

        /** Returns the region's renderer-internal image. */
        OverlayImage image() {
            return image;
        }

        /** Returns the normalized horizontal origin. */
        float minimumU() {
            return minimumU;
        }

        /** Returns the normalized vertical origin. */
        float minimumV() {
            return minimumV;
        }

        /** Returns the normalized horizontal endpoint. */
        float maximumU() {
            return maximumU;
        }

        /** Returns the normalized vertical endpoint. */
        float maximumV() {
            return maximumV;
        }
    }
}
