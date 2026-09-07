/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.lwjgl.internal.Preconditions;

/**
 * Immutable physical and logical dimensions of one render surface.
 *
 * <p>Framebuffer dimensions are physical pixels. Logical dimensions use the host interface's
 * coordinate system and are used for overlays. Zero is valid for any dimension while a surface is
 * collapsed, hidden, or otherwise temporarily not drawable.
 *
 * @param framebufferWidth framebuffer width in physical pixels
 * @param framebufferHeight framebuffer height in physical pixels
 * @param logicalWidth surface width in host logical coordinates
 * @param logicalHeight surface height in host logical coordinates
 */
public record RenderSurfaceSize(int framebufferWidth, int framebufferHeight, int logicalWidth, int logicalHeight) {
    /** Validates that every dimension is non-negative. */
    public RenderSurfaceSize {
        Preconditions.requireNonNegative(framebufferWidth, "framebufferWidth");
        Preconditions.requireNonNegative(framebufferHeight, "framebufferHeight");
        Preconditions.requireNonNegative(logicalWidth, "logicalWidth");
        Preconditions.requireNonNegative(logicalHeight, "logicalHeight");
    }

    /**
     * Returns whether the surface currently has physical and logical area.
     *
     * @return {@code true} when every dimension is positive
     */
    public boolean isDrawable() {
        return framebufferWidth > 0 && framebufferHeight > 0 && logicalWidth > 0 && logicalHeight > 0;
    }
}
