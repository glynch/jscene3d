/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

/** Identifies the image corner represented by the texture coordinate {@code (0, 0)}. */
public enum TextureCoordinateOrigin {
    /** The first coordinate addresses the lower-left image corner. */
    BOTTOM_LEFT,
    /** The first coordinate addresses the upper-left image corner, as required by glTF. */
    TOP_LEFT
}
