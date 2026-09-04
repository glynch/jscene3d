/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeObject;

/** Runtime object that contributes one spatial object to a declarative 3d scene. */
public interface Scene3dRuntimeObject extends ProjectRuntimeObject {
    /**
     * Returns the scene-graph object used for authored parent-child attachment.
     *
     * @return retained JScene3D spatial object
     */
    Object3D object3d();
}
