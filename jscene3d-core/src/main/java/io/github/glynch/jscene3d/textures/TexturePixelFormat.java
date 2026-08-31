/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

/** Describes the channel layout and storage of one texture pixel. */
public enum TexturePixelFormat {
    /** Four unsigned eight-bit channels in red, green, blue, alpha order. */
    RGBA8(4);

    private final int bytesPerPixel;

    /** Retains the fixed storage size for one pixel. */
    TexturePixelFormat(int bytesPerPixel) {
        this.bytesPerPixel = bytesPerPixel;
    }

    /**
     * Returns the storage size of one pixel.
     *
     * @return bytes per pixel
     */
    public int bytesPerPixel() {
        return bytesPerPixel;
    }
}
