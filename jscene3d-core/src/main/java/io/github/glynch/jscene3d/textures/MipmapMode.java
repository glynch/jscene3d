/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

/** Selects whether each renderer generates mipmap levels for a texture. */
public enum MipmapMode {
    /** Generates the complete mipmap chain after each image upload. */
    GENERATE,

    /** Keeps only the supplied base image and requires a non-mipmap minification filter. */
    NONE
}
