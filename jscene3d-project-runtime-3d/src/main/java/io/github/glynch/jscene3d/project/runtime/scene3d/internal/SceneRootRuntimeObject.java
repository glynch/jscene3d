/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d.internal;

import io.github.glynch.jscene3d.game.FrameUpdate;
import io.github.glynch.jscene3d.project.runtime.RenderParticipant;
import io.github.glynch.jscene3d.scenes.Scene;

/** Root scene object that delegates one render submission per runtime frame. */
final class SceneRootRuntimeObject extends SpatialRuntimeObject implements RenderParticipant {
    private final Scene3dComposition composition;

    /** Stores the completed-scene composition used during startup and rendering. */
    SceneRootRuntimeObject(Scene scene, Scene3dComposition composition) {
        super(scene);
        this.composition = composition;
    }

    @Override
    public void start() {
        composition.requireActiveCamera();
    }

    @Override
    public void render(FrameUpdate update) {
        composition.render();
    }
}
