/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.process.SceneCameraIconState.Bounds;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import io.github.glynch.jscene3d.render.OverlayTextAlignment;

/** Paints compact, asset-free authored-camera symbols into the safe overlay canvas. */
final class SceneCameraIconRenderer {
    private static final Color BODY = Color.srgb(0xd7dbe5);
    private static final Color BODY_SHADOW = Color.srgb(0x161a22);
    private static final Color SELECTED = Color.srgb(0xb69cff);
    private static final Color BADGE = Color.srgb(0x7257d9);

    /** Paints one camera symbol with selection and camera-multiplicity feedback. */
    void paint(OverlayCanvas canvas, SceneCameraIconState state) {
        Bounds bounds = state.bounds();
        Color color = state.selected() ? SELECTED : BODY;
        canvas.roundedRectangle(
                bounds.x() - 2.0f,
                bounds.y() - 2.0f,
                bounds.width() + 4.0f,
                bounds.height() + 4.0f,
                4.0f,
                BODY_SHADOW,
                0.76f);
        float bodyWidth = bounds.width() * 0.64f;
        canvas.roundedRectangle(bounds.x(), bounds.y() + 4.0f, bodyWidth, bounds.height() - 8.0f, 3.0f, color, 1.0f);
        canvas.triangle(
                new OverlayCanvas.Triangle(
                        bounds.x() + bodyWidth,
                        bounds.y() + 8.0f,
                        bounds.x() + bounds.width(),
                        bounds.y() + 3.0f,
                        bounds.x() + bounds.width(),
                        bounds.y() + bounds.height() - 3.0f),
                color,
                1.0f);
        if (state.hasOrthographic()) {
            float lineX = bounds.x() + 5.0f;
            canvas.line(lineX, bounds.y() + 8.0f, lineX, bounds.y() + bounds.height() - 8.0f, 2.0f, BODY_SHADOW, 0.72f);
        }
        if (state.cameraCount() > 1) {
            float diameter = 12.0f;
            float badgeX = bounds.x() + bounds.width() - diameter * 0.5f;
            float badgeY = bounds.y() - diameter * 0.5f;
            canvas.roundedRectangle(badgeX, badgeY, diameter, diameter, diameter * 0.5f, BADGE, 1.0f);
            canvas.text(
                    Integer.toString(state.cameraCount()),
                    badgeX + diameter * 0.5f,
                    badgeY - 1.0f,
                    10.0f,
                    Color.WHITE,
                    1.0f,
                    OverlayTextAlignment.CENTER);
        }
    }
}
