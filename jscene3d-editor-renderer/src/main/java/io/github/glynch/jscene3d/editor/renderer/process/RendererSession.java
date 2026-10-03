/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;

/** One Java rendering session owned by one exact Electron native session. */
interface RendererSession extends AutoCloseable {
    void renderFrame();

    SurfaceSize receiveReplacementSurface();

    void applyValidationDrag(float horizontal, float vertical);

    void replaceSceneViewSnapshot(SceneViewSnapshot snapshot);

    @Override
    void close();

    record SurfaceSize(int width, int height) {
        public SurfaceSize {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Surface dimensions must be positive");
            }
        }
    }
}
