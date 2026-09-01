/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.objects.InstancedMesh;
import io.github.glynch.jscene3d.render.internal.resources.GeometryResource;
import io.github.glynch.jscene3d.render.internal.resources.InstanceResource;
import io.github.glynch.jscene3d.render.internal.resources.MorphResources;
import java.util.Map;
import java.util.Set;

/**
 * Renderer-owned caches shared with shadow passes for the duration of one frame.
 *
 * @param geometries realized geometry resources
 * @param instances realized instance resources
 * @param activeInstances instances used during the current frame
 * @param morphs shared morph-target resources
 */
public record ShadowResourceContext(
        Map<BufferGeometry, GeometryResource> geometries,
        Map<InstancedMesh, InstanceResource> instances,
        Set<InstancedMesh> activeInstances,
        MorphResources morphs) {}
