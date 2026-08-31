/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

/** Selects texel interpolation and, for minification, optional mipmap interpolation. */
public enum TextureFilter {
    /** Selects the nearest texel without mipmaps. */
    NEAREST(false),

    /** Linearly interpolates neighboring texels without mipmaps. */
    LINEAR(false),

    /** Selects the nearest texel from the nearest mipmap level. */
    NEAREST_MIPMAP_NEAREST(true),

    /** Linearly interpolates texels from the nearest mipmap level. */
    LINEAR_MIPMAP_NEAREST(true),

    /** Selects nearest texels and linearly interpolates between mipmap levels. */
    NEAREST_MIPMAP_LINEAR(true),

    /** Linearly interpolates texels and mipmap levels. */
    LINEAR_MIPMAP_LINEAR(true);

    private final boolean usesMipmaps;

    /** Retains whether this filter samples mipmap levels. */
    TextureFilter(boolean usesMipmaps) {
        this.usesMipmaps = usesMipmaps;
    }

    /**
     * Returns whether this filter samples mipmap levels.
     *
     * @return {@code true} for a mipmap minification filter
     */
    public boolean usesMipmaps() {
        return usesMipmaps;
    }
}
