/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui.internal;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.render.OverlayImage;

/** Internal drawing boundary that keeps GUI layout testable without a native renderer. */
public interface GuiCanvas {
    /**
     * Appends a solid rectangle.
     *
     * @param x left coordinate
     * @param y top coordinate
     * @param width rectangle width
     * @param height rectangle height
     * @param color fill color
     * @param alpha fill opacity
     */
    void rectangle(float x, float y, float width, float height, Color color, float alpha);

    /**
     * Appends a solid rounded rectangle.
     *
     * @param x left coordinate
     * @param y top coordinate
     * @param width rectangle width
     * @param height rectangle height
     * @param radius corner radius
     * @param color fill color
     * @param alpha fill opacity
     */
    void roundedRectangle(float x, float y, float width, float height, float radius, Color color, float alpha);

    /**
     * Appends a solid line segment.
     *
     * @param startX starting horizontal coordinate
     * @param startY starting vertical coordinate
     * @param endX ending horizontal coordinate
     * @param endY ending vertical coordinate
     * @param thickness line thickness
     * @param color line color
     * @param alpha line opacity
     */
    void line(float startX, float startY, float endX, float endY, float thickness, Color color, float alpha);

    /**
     * Appends a tinted alpha-mask image region.
     *
     * @param region image region to draw
     * @param x left coordinate
     * @param y top coordinate
     * @param width image width
     * @param height image height
     * @param color tint color
     * @param alpha image opacity
     */
    void alphaMask(OverlayImage.Region region, float x, float y, float width, float height, Color color, float alpha);

    /**
     * Appends a tinted full-color image region.
     *
     * @param region image region to draw
     * @param x left coordinate
     * @param y top coordinate
     * @param width image width
     * @param height image height
     * @param tint multiplicative tint color
     * @param alpha image opacity
     */
    void image(OverlayImage.Region region, float x, float y, float width, float height, Color tint, float alpha);
}
