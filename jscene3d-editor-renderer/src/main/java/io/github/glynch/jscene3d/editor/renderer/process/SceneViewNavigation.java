/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

/** Validated editor-camera operation received from the owning Scene View. */
sealed interface SceneViewNavigation {
    record Orbit(float horizontal, float vertical, int viewportWidth, int viewportHeight)
            implements SceneViewNavigation {}

    record Pan(float horizontal, float vertical, int viewportWidth, int viewportHeight)
            implements SceneViewNavigation {}

    record Zoom(float amount) implements SceneViewNavigation {}

    record GizmoClick(float horizontal, float vertical, int viewportWidth, int viewportHeight)
            implements SceneViewNavigation {}

    record FocusSelected() implements SceneViewNavigation {}

    record FrameAll() implements SceneViewNavigation {}

    record ToggleProjection() implements SceneViewNavigation {}
}
