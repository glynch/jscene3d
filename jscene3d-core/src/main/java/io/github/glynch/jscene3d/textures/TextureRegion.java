/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

import io.github.glynch.jscene3d.internal.Preconditions;

/**
 * Immutable normalized rectangular region of a texture image.
 *
 * @param u horizontal lower-left coordinate
 * @param v vertical lower-left coordinate
 * @param width normalized width
 * @param height normalized height
 */
public record TextureRegion(float u, float v, float width, float height) {
    private static final TextureRegion FULL = new TextureRegion(0.0f, 0.0f, 1.0f, 1.0f);

    /**
     * Creates a normalized region measured from the lower-left texture-coordinate corner.
     *
     * @param u horizontal lower-left coordinate in the inclusive range from zero through one
     * @param v vertical lower-left coordinate in the inclusive range from zero through one
     * @param width finite positive normalized width
     * @param height finite positive normalized height
     * @throws IllegalArgumentException if the region is non-finite, non-positive, or outside the
     *     texture
     */
    public TextureRegion {
        u = Preconditions.requireInRange(u, 0.0f, 1.0f, "u");
        v = Preconditions.requireInRange(v, 0.0f, 1.0f, "v");
        width = Preconditions.requirePositive(width, "width");
        height = Preconditions.requirePositive(height, "height");
        if (u + width > 1.0f || v + height > 1.0f) {
            throw new IllegalArgumentException("texture region must remain inside normalized texture coordinates");
        }
    }

    /**
     * Returns the complete texture region.
     *
     * @return shared immutable region spanning the entire texture
     */
    public static TextureRegion full() {
        return FULL;
    }

    /**
     * Converts top-row-first image pixels into a normalized texture region.
     *
     * <p>This factory matches image editors and sprite-sheet importers, where {@code (0, 0)} is
     * the upper-left pixel. The resulting region uses the lower-left normalized coordinates
     * consumed by renderers.
     *
     * @param x non-negative left pixel
     * @param y non-negative top pixel
     * @param width positive region width in pixels
     * @param height positive region height in pixels
     * @param textureWidth positive complete texture width in pixels
     * @param textureHeight positive complete texture height in pixels
     * @return normalized immutable region
     * @throws IllegalArgumentException if dimensions are invalid or the region exceeds the texture
     */
    public static TextureRegion fromPixels(int x, int y, int width, int height, int textureWidth, int textureHeight) {
        int validX = Preconditions.requireNonNegative(x, "x");
        int validY = Preconditions.requireNonNegative(y, "y");
        int validWidth = Preconditions.requirePositive(width, "width");
        int validHeight = Preconditions.requirePositive(height, "height");
        int validTextureWidth = Preconditions.requirePositive(textureWidth, "textureWidth");
        int validTextureHeight = Preconditions.requirePositive(textureHeight, "textureHeight");
        if ((long) validX + validWidth > validTextureWidth || (long) validY + validHeight > validTextureHeight) {
            throw new IllegalArgumentException("pixel region must remain inside the texture dimensions");
        }
        float normalizedU = validX / (float) validTextureWidth;
        float normalizedV = (validTextureHeight - validY - validHeight) / (float) validTextureHeight;
        return new TextureRegion(
                normalizedU,
                normalizedV,
                validWidth / (float) validTextureWidth,
                validHeight / (float) validTextureHeight);
    }
}
