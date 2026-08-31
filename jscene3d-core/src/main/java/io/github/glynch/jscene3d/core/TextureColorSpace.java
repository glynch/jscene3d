/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Describes how RGBA texture color channels are interpreted during sampling. */
public enum TextureColorSpace {
    /** sRGB-encoded color channels converted to linear sRGB by the renderer. */
    SRGB,

    /** Linear channels sampled without color conversion. */
    LINEAR
}
