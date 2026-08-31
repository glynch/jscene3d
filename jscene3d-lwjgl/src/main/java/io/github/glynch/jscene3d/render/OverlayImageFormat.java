/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Renderer-internal pixel layout and color interpretation for immutable overlay images. */
enum OverlayImageFormat {
    /** One unsigned byte interpreted only as opacity. */
    ALPHA_MASK(1),

    /** Four unsigned bytes interpreted as sRGB red, green, blue, and linear alpha. */
    SRGB_RGBA(4);

    private final int componentCount;

    /** Associates a format with its tightly packed component count. */
    OverlayImageFormat(int componentCount) {
        this.componentCount = componentCount;
    }

    /** Returns the number of bytes stored for each pixel. */
    int componentCount() {
        return componentCount;
    }
}
