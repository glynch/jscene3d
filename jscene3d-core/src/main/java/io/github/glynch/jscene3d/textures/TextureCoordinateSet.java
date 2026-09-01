/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

/** Geometry texture-coordinate attribute selected independently by a material map role. */
public enum TextureCoordinateSet {
    /** Primary {@code uv} attribute corresponding to glTF {@code TEXCOORD_0}. */
    PRIMARY,
    /** Secondary {@code uv1} attribute corresponding to glTF {@code TEXCOORD_1}. */
    SECONDARY
}
