/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

/** Selects how texture coordinates outside the unit interval address an image. */
public enum TextureWrap {
    /** Clamps coordinates to the nearest image edge. */
    CLAMP_TO_EDGE,

    /** Repeats the image at every integer texture-coordinate boundary. */
    REPEAT,

    /** Repeats the image while mirroring every other repetition. */
    MIRRORED_REPEAT
}
