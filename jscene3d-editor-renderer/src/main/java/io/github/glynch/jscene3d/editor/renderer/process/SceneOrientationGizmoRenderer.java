/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.AxisVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.CubeFaceVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.CubeVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Point;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.TipStyle;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import io.github.glynch.jscene3d.render.OverlayTextAlignment;
import java.util.List;

/** Paints immutable Scene orientation gizmo state through generic overlay primitives. */
final class SceneOrientationGizmoRenderer {
    private static final float CUBE_EDGE_THICKNESS = 2.0f;
    private static final Color CUBE_EDGE = Color.srgb(0x596273);
    private static final Color LABEL_COLOR = Color.WHITE;
    private static final Color LABEL_SHADOW = Color.BLACK;

    /** Paints axes back-to-front around the central cube, followed by labels. */
    void paint(OverlayCanvas canvas, SceneOrientationGizmoState state) {
        state.axes().stream().filter(axis -> axis.depth() <= 0.0f).forEach(axis -> paintAxis(canvas, axis));
        paintCube(canvas, state.cube());
        state.axes().stream().filter(axis -> axis.depth() > 0.0f).forEach(axis -> paintAxis(canvas, axis));
        state.axes().stream().filter(axis -> axis.label().isPresent()).forEach(axis -> paintLabel(canvas, axis));
    }

    /** Draws one directed axis using its retained stem and tip geometry. */
    private static void paintAxis(OverlayCanvas canvas, AxisVisual axis) {
        if (axis.tipStyle() == TipStyle.END_ON) {
            circle(canvas, axis.endpoint(), axis.tipSize(), axis.color(), axis.alpha());
            return;
        }
        canvas.line(
                axis.stemStart().x(),
                axis.stemStart().y(),
                axis.stemEnd().x(),
                axis.stemEnd().y(),
                axis.stemThickness(),
                axis.color(),
                axis.alpha());
        List<Point> head = axis.head();
        canvas.triangle(overlayTriangle(head.get(0), head.get(1), head.get(2)), axis.color(), axis.alpha());
    }

    /** Draws the central orientation cube back-to-front with restrained edge contrast. */
    private static void paintCube(OverlayCanvas canvas, CubeVisual cube) {
        for (CubeFaceVisual face : cube.faces()) {
            List<Point> points = face.points();
            triangle(canvas, points.get(0), points.get(1), points.get(2), face.color(), 1.0f);
            triangle(canvas, points.get(0), points.get(2), points.get(3), face.color(), 1.0f);
            for (int index = 0; index < points.size(); index++) {
                Point start = points.get(index);
                Point end = points.get((index + 1) % points.size());
                canvas.line(start.x(), start.y(), end.x(), end.y(), CUBE_EDGE_THICKNESS, CUBE_EDGE, 0.82f);
            }
        }
    }

    /** Draws one positive-axis label with generic antialiased overlay text and a contrast shadow. */
    private static void paintLabel(OverlayCanvas canvas, AxisVisual axis) {
        String label = axis.label().orElseThrow();
        Point anchor = axis.labelAnchor();
        canvas.text(
                label,
                anchor.x() + 1.0f,
                anchor.y() + 1.0f,
                SceneOrientationGizmoLayout.LABEL_FONT_SIZE,
                LABEL_SHADOW,
                0.82f,
                OverlayTextAlignment.CENTER);
        canvas.text(
                label,
                anchor.x(),
                anchor.y(),
                SceneOrientationGizmoLayout.LABEL_FONT_SIZE,
                LABEL_COLOR,
                1.0f,
                OverlayTextAlignment.CENTER);
    }

    /** Draws a circle through the overlay canvas's maximally rounded rectangle primitive. */
    private static void circle(OverlayCanvas canvas, Point center, float diameter, Color color, float alpha) {
        float radius = diameter * 0.5f;
        canvas.roundedRectangle(center.x() - radius, center.y() - radius, diameter, diameter, radius, color, alpha);
    }

    /** Draws one solid triangle from semantic points. */
    private static void triangle(
            OverlayCanvas canvas, Point first, Point second, Point third, Color color, float alpha) {
        canvas.triangle(overlayTriangle(first, second, third), color, alpha);
    }

    /** Converts semantic gizmo points to one reusable overlay-canvas triangle value. */
    private static OverlayCanvas.Triangle overlayTriangle(Point first, Point second, Point third) {
        return new OverlayCanvas.Triangle(first.x(), first.y(), second.x(), second.y(), third.x(), third.y());
    }
}
