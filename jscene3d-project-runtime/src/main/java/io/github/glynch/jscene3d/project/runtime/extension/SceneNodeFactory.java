/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeObject;

/** Creates executable instances of one registered scene-node type. */
@FunctionalInterface
public interface SceneNodeFactory {
    /**
     * Creates one runtime object without starting it.
     *
     * @param context bounded creation context
     * @return newly owned runtime object
     */
    ProjectRuntimeObject create(SceneNodeContext context);
}
