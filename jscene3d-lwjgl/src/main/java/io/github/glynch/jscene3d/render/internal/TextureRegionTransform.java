/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import io.github.glynch.jscene3d.textures.TextureRegion;
import org.joml.Matrix3f;

/** Composes a normalized atlas region after a texture's configured UV transform. */
public final class TextureRegionTransform {
    /** Prevents instantiation of this stateless transform helper. */
    private TextureRegionTransform() {
        throw new AssertionError("TextureRegionTransform cannot be instantiated");
    }

    /**
     * Composes one normalized atlas region into a caller-owned UV transform matrix.
     *
     * @param matrix mutable texture transform receiving the region composition
     * @param region normalized atlas region applied after the existing transform
     * @return {@code matrix}
     */
    public static Matrix3f apply(Matrix3f matrix, TextureRegion region) {
        float m00 = matrix.m00();
        float m01 = matrix.m01();
        float m10 = matrix.m10();
        float m11 = matrix.m11();
        float m20 = matrix.m20();
        float m21 = matrix.m21();
        return matrix.set(
                region.width() * m00,
                region.height() * m01,
                0.0f,
                region.width() * m10,
                region.height() * m11,
                0.0f,
                region.u() + region.width() * m20,
                region.v() + region.height() * m21,
                1.0f);
    }
}
