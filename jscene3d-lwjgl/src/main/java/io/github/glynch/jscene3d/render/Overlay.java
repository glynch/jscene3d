/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/**
 * Paints a logical-coordinate overlay without receiving access to OpenGL state.
 *
 * <p>The renderer supplies a cleared canvas for every invocation. Coordinates use logical window
 * pixels with the origin at the upper-left corner.
 */
@FunctionalInterface
public interface Overlay {
    /**
     * Appends this overlay's drawing commands for the current frame.
     *
     * @param canvas renderer-owned logical-coordinate canvas
     * @param width current logical window width
     * @param height current logical window height
     */
    void paint(OverlayCanvas canvas, int width, int height);
}
