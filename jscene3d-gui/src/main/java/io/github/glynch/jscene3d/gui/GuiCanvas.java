/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.render.OverlayImage;

/** Internal drawing boundary that keeps GUI layout testable without a native renderer. */
interface GuiCanvas {
    /** Appends a solid rectangle. */
    void rectangle(float x, float y, float width, float height, Color color, float alpha);

    /** Appends a solid rounded rectangle. */
    void roundedRectangle(float x, float y, float width, float height, float radius, Color color, float alpha);

    /** Appends a solid line segment. */
    void line(float startX, float startY, float endX, float endY, float thickness, Color color, float alpha);

    /** Appends a tinted alpha-mask image region. */
    void alphaMask(OverlayImage.Region region, float x, float y, float width, float height, Color color, float alpha);
}
