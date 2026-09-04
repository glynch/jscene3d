/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeObject;

/** Creates executable instances of one registered node-controller type. */
@FunctionalInterface
public interface NodeControllerFactory {
    /**
     * Creates one controller without starting it.
     *
     * @param context bounded creation context
     * @return newly owned controller object
     */
    ProjectRuntimeObject create(NodeControllerContext context);
}
