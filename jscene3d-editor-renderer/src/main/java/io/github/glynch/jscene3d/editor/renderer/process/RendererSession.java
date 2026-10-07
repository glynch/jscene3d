/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import java.util.Optional;

/** One Java rendering session owned by one exact Electron native session. */
interface RendererSession extends AutoCloseable {
    void renderFrame();

    SurfaceSize receiveReplacementSurface();

    void applyValidationDrag(float horizontal, float vertical);

    void replaceSceneViewSnapshot(SceneViewSnapshot snapshot);

    default SceneViewSelectionResult pickSceneView(long revision, float horizontal, float vertical) {
        throw new IllegalStateException("Renderer session does not support Scene View picking");
    }

    default boolean selectSceneView(long revision, Optional<CompositionOccurrenceId> occurrence) {
        throw new IllegalStateException("Renderer session does not support Scene View selection");
    }

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
