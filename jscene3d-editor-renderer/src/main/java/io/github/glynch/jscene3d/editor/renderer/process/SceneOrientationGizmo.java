/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import java.util.Objects;

/** Session-owned Scene View overlay that visualizes the editor camera's world orientation. */
final class SceneOrientationGizmo implements Overlay, AutoCloseable {
    static final float WIDTH = SceneOrientationGizmoLayout.WIDTH;
    static final float HEIGHT = SceneOrientationGizmoLayout.HEIGHT;
    static final float MARGIN = SceneOrientationGizmoLayout.MARGIN;

    private final SceneOrientationGizmoLayout layout;
    private final SceneOrientationGizmoRenderer renderer = new SceneOrientationGizmoRenderer();
    private boolean closed;

    /** Retains the editor camera whose orientation is sampled for each rendered frame. */
    SceneOrientationGizmo(Camera camera) {
        layout = new SceneOrientationGizmoLayout(Objects.requireNonNull(camera, "camera"));
    }

    /** Paints the orientation control into the renderer's logical-coordinate overlay. */
    @Override
    public void paint(OverlayCanvas canvas, int width, int height) {
        requireOpen();
        Objects.requireNonNull(canvas, "canvas");
        if (width > 0 && height > 0) {
            renderer.paint(canvas, layout.renderState(width, height));
        }
    }

    /** Builds immutable screen-space state for testing and future hit testing. */
    SceneOrientationGizmoState renderState(int viewportWidth, int viewportHeight) {
        requireOpen();
        return layout.renderState(viewportWidth, viewportHeight);
    }

    /** Returns whether this gizmo has released its session lifecycle. */
    boolean isClosed() {
        return closed;
    }

    /** Releases this session-owned gizmo. */
    @Override
    public void close() {
        closed = true;
    }

    /** Rejects use after the owning renderer session has closed. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Scene orientation gizmo is closed");
        }
    }
}
