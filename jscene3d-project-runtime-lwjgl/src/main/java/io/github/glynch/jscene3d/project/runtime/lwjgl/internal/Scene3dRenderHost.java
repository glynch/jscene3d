/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl.internal;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.scenes.Scene;

/** Internal seam between declarative scene composition and a graphical render host. */
public interface Scene3dRenderHost {
    /**
     * Renders one scene through its active camera when a drawable surface is available.
     *
     * @param scene composed scene
     * @param camera active perspective camera
     */
    void render(Scene scene, PerspectiveCamera camera);
}
