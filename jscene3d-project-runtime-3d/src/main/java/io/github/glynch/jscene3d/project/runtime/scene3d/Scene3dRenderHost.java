/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.scenes.Scene;

/** Receives render submissions from one composed native 3d project runtime. */
public interface Scene3dRenderHost {
    /**
     * Renders one scene through its active camera when a drawable surface is available.
     *
     * @param scene composed scene
     * @param camera active perspective camera
     */
    void render(Scene scene, PerspectiveCamera camera);
}
