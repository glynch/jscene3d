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
    private static final Color OUTLINE = Color.srgb(0x161a22);
    private static final Color SELECTED = Color.srgb(0xb69cff);
    private static final Color BADGE = Color.srgb(0x7257d9);

    /** Paints one camera symbol with selection and camera-multiplicity feedback. */
    void paint(OverlayCanvas canvas, SceneCameraIconState state) {
        Bounds bounds = state.bounds();
        Silhouette silhouette = silhouette(bounds);
        Appearance appearance = appearance(state.selected());
        Bounds body = silhouette.body();
        Bounds viewfinder = silhouette.viewfinder();
        Lens lens = silhouette.lens();

        canvas.roundedRectangle(
                viewfinder.x() - 1.0f,
                viewfinder.y() - 1.0f,
                viewfinder.width() + 2.0f,
                viewfinder.height() + 2.0f,
                2.0f,
                appearance.outline(),
                0.92f);
        canvas.roundedRectangle(
                body.x() - 1.0f,
                body.y() - 1.0f,
                body.width() + 2.0f,
                body.height() + 2.0f,
                3.0f,
                appearance.outline(),
                0.92f);
        canvas.triangle(triangle(lens.outlined()), appearance.outline(), 0.92f);
        canvas.roundedRectangle(
                viewfinder.x(), viewfinder.y(), viewfinder.width(), viewfinder.height(), 1.0f, appearance.fill(), 1.0f);
        canvas.roundedRectangle(body.x(), body.y(), body.width(), body.height(), 2.0f, appearance.fill(), 1.0f);
        canvas.triangle(triangle(lens), appearance.fill(), 1.0f);

        if (state.cameraCount() > 1) {
            float diameter = 9.0f;
            float badgeX = bounds.x() + bounds.width() - diameter;
            float badgeY = bounds.y();
            canvas.roundedRectangle(badgeX, badgeY, diameter, diameter, diameter * 0.5f, BADGE, 1.0f);
            canvas.text(
                    Integer.toString(state.cameraCount()),
                    badgeX + diameter * 0.5f,
                    badgeY - 0.5f,
                    8.0f,
                    Color.WHITE,
                    1.0f,
                    OverlayTextAlignment.CENTER);
        }
    }

    /** Returns the selection-dependent palette without allocating renderer resources. */
    static Appearance appearance(boolean selected) {
        return new Appearance(selected ? SELECTED : BODY, OUTLINE);
    }

    /** Lays out a compact body, raised viewfinder, and forward lens cone. */
    static Silhouette silhouette(Bounds bounds) {
        return new Silhouette(
                new Bounds(bounds.x() + 2.0f, bounds.y() + 6.0f, 13.0f, 11.0f),
                new Bounds(bounds.x() + 5.0f, bounds.y() + 3.0f, 7.0f, 4.0f),
                new Lens(
                        bounds.x() + 15.0f,
                        bounds.y() + 7.0f,
                        bounds.x() + 22.0f,
                        bounds.y() + 4.0f,
                        bounds.x() + 22.0f,
                        bounds.y() + 18.0f));
    }

    private static OverlayCanvas.Triangle triangle(Lens lens) {
        return new OverlayCanvas.Triangle(
                lens.baseX(), lens.baseY(), lens.topX(), lens.topY(), lens.bottomX(), lens.bottomY());
    }

    /** Immutable icon fill and contrasting outline colours. */
    record Appearance(Color fill, Color outline) {}

    /** Immutable semantic parts of the camera silhouette. */
    record Silhouette(Bounds body, Bounds viewfinder, Lens lens) {}

    /** Immutable camera-lens cone triangle. */
    record Lens(float baseX, float baseY, float topX, float topY, float bottomX, float bottomY) {
        /** Expands the contrast outline while keeping it inside the icon envelope. */
        Lens outlined() {
            return new Lens(baseX - 0.5f, baseY - 1.0f, topX + 1.0f, topY - 1.0f, bottomX + 1.0f, bottomY + 1.0f);
        }
    }
}
