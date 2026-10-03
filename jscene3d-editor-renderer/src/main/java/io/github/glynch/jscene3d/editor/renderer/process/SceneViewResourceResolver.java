/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.project.value.ResourceReference;

/** Fixed product-owned resource boundary used by safe Scene View realization. */
interface SceneViewResourceResolver {
    Lease<BufferGeometry> acquireMesh(ResourceReference reference);

    Lease<Material> acquireMaterial(ResourceReference reference);

    /** One independently releasable retention of a shared renderer resource. */
    interface Lease<T> extends AutoCloseable {
        T value();

        @Override
        void close();
    }
}
