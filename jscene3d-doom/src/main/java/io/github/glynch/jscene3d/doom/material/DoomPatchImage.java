/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.material;

import java.util.Objects;

/**
 * Decoded Doom patch image and its source-space origin offsets.
 *
 * @param image decoded RGBA image
 * @param leftOffset horizontal source origin offset
 * @param topOffset vertical source origin offset
 */
public record DoomPatchImage(RgbaImage image, int leftOffset, int topOffset) {
    /** Creates an immutable decoded patch. */
    public DoomPatchImage {
        Objects.requireNonNull(image, "image");
    }
}
