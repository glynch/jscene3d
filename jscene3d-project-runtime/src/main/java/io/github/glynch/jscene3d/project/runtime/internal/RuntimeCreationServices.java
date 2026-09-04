/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;

/**
 * Shared project and runtime services supplied to every factory context in one composition.
 *
 * @param project validated project manifest
 * @param scene scene being composed
 * @param router scene endpoint router
 * @param resources project resource resolver
 */
record RuntimeCreationServices(
        GameProject project, SceneDefinition scene, EndpointRouter router, ProjectResourceResolver resources) {}
