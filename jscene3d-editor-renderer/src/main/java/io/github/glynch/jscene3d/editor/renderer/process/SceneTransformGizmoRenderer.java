/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Axis;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.AxisVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Handle;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Point;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Paints immutable transform-gizmo state into the renderer overlay. */
final class SceneTransformGizmoRenderer {
    private static final Color X_COLOR = Color.srgb(0xf04b52);
    private static final Color Y_COLOR = Color.srgb(0x5dcc58);
    private static final Color Z_COLOR = Color.srgb(0x4b79ff);
    private static final Color HOVER_COLOR = Color.srgb(0xffe45c);
    private static final Color ACTIVE_COLOR = Color.srgb(0xfff29a);
    private static final Color DISABLED_COLOR = Color.srgb(0x79808d);

    /** Paints one frame with hover, drag, and authored-editability feedback. */
    void paint(
            OverlayCanvas canvas,
            SceneTransformGizmoState state,
            @Nullable Handle hovered,
            @Nullable Handle active,
            boolean editable) {
        if (state.tool() == SceneTransformTool.ROTATE) {
            paintRotation(canvas, state, hovered, active, editable);
        } else {
            paintLinear(canvas, state, hovered, active, editable);
        }
    }

    /** Paints move or scale axes plus the tool-specific auxiliary handles. */
    private static void paintLinear(
            OverlayCanvas canvas,
            SceneTransformGizmoState state,
            @Nullable Handle hovered,
            @Nullable Handle active,
            boolean editable) {
        if (state.tool() == SceneTransformTool.MOVE) {
            paintPlanes(canvas, state, hovered, active, editable);
        }
        for (AxisVisual axis : state.axes()) {
            Handle handle = Handle.axis(axis.axis());
            Color color = color(axis.axis(), handle, hovered, active, editable);
            Point endpoint = state.endpoint(axis.axis());
            canvas.line(
                    state.center().x(),
                    state.center().y(),
                    endpoint.x(),
                    endpoint.y(),
                    lineThickness(handle, hovered, active),
                    color,
                    1.0f);
            if (state.tool() == SceneTransformTool.MOVE) {
                float handleScale = handleScale(handle, hovered, active);
                float normalX = -axis.screenY() * 10.0f * handleScale;
                float normalY = axis.screenX() * 10.0f * handleScale;
                canvas.triangle(
                        new OverlayCanvas.Triangle(
                                endpoint.x() + axis.screenX() * 14.0f * handleScale,
                                endpoint.y() + axis.screenY() * 14.0f * handleScale,
                                endpoint.x() + normalX,
                                endpoint.y() + normalY,
                                endpoint.x() - normalX,
                                endpoint.y() - normalY),
                        color,
                        1.0f);
            } else {
                float size = 16.0f * handleScale(handle, hovered, active);
                canvas.rectangle(endpoint.x() - size * 0.5f, endpoint.y() - size * 0.5f, size, size, color, 1.0f);
            }
        }
        if (state.tool() == SceneTransformTool.SCALE) {
            Color color = color(Axis.X, Handle.UNIFORM, hovered, active, editable);
            float size = 20.0f * handleScale(Handle.UNIFORM, hovered, active);
            canvas.rectangle(
                    state.center().x() - size * 0.5f, state.center().y() - size * 0.5f, size, size, color, 1.0f);
        }
    }

    /** Paints three planar move handles. */
    private static void paintPlanes(
            OverlayCanvas canvas,
            SceneTransformGizmoState state,
            @Nullable Handle hovered,
            @Nullable Handle active,
            boolean editable) {
        for (Handle plane : List.of(Handle.XY, Handle.XZ, Handle.YZ)) {
            Point point = state.planeCenter(plane);
            Color color = color(plane.axes().get(0), plane, hovered, active, editable);
            float size = 20.0f * handleScale(plane, hovered, active);
            canvas.rectangle(point.x() - size * 0.5f, point.y() - size * 0.5f, size, size, color, 0.76f);
        }
    }

    /** Paints projected world- or local-axis rotation rings. */
    private static void paintRotation(
            OverlayCanvas canvas,
            SceneTransformGizmoState state,
            @Nullable Handle hovered,
            @Nullable Handle active,
            boolean editable) {
        for (AxisVisual axis : state.axes()) {
            Handle handle = Handle.axis(axis.axis());
            Color color = color(axis.axis(), handle, hovered, active, editable);
            Point previous = null;
            for (int segment = 0; segment <= 64; segment++) {
                float angle = (float) (Math.PI * 2.0 * segment / 64.0);
                Point point = state.ringPoint(axis.axis(), angle);
                if (previous != null) {
                    canvas.line(
                            previous.x(),
                            previous.y(),
                            point.x(),
                            point.y(),
                            lineThickness(handle, hovered, active),
                            color,
                            0.98f);
                }
                previous = point;
            }
        }
    }

    /** Resolves one handle's conventional axis, hover, active, or disabled color. */
    static Color color(Axis axis, Handle handle, @Nullable Handle hovered, @Nullable Handle active, boolean editable) {
        if (!editable) {
            return DISABLED_COLOR;
        }
        if (active == handle) {
            return ACTIVE_COLOR;
        }
        if (hovered == handle) {
            return HOVER_COLOR;
        }
        return switch (axis) {
            case X -> X_COLOR;
            case Y -> Y_COLOR;
            case Z -> Z_COLOR;
        };
    }

    /** Returns a stronger stroke for the active handle and a clear stroke for hover. */
    private static float lineThickness(Handle handle, @Nullable Handle hovered, @Nullable Handle active) {
        if (active == handle) {
            return 7.0f;
        }
        return hovered == handle ? 5.5f : 4.0f;
    }

    /** Returns a modest visual emphasis without changing the handle's stable screen-space anchor. */
    private static float handleScale(Handle handle, @Nullable Handle hovered, @Nullable Handle active) {
        if (active == handle) {
            return 1.3f;
        }
        return hovered == handle ? 1.15f : 1.0f;
    }
}
