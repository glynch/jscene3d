/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

/** Closed internal identity shared by scene-definition and entity-component world routing. */
sealed interface EndpointAddress permits RuntimeEndpointAddress, SceneEndpointAddress {}
