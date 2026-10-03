/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.PublishedRuntimeResources;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.spatial3d.Material3dResource;
import io.github.glynch.jscene3d.project.spatial3d.Mesh3dResource;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dResourceLoaders;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves only fixed product-owned spatial resources from authored and published project content. */
final class ProductSceneViewResourceResolver implements SceneViewResourceResolver {
    private final RuntimeResourceProvider resources;

    /** Stores the exact fixed provider; exposed for focused boundary tests. */
    ProductSceneViewResourceResolver(RuntimeResourceProvider resources) {
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    /** Creates a resolver without discovering or executing project resource-loader code. */
    static ProductSceneViewResourceResolver create(
            GameProject project, RegisteredTypeCatalog types, Path publishedContentRoot) {
        RuntimeResourceProvider provider = PublishedRuntimeResources.load(
                Objects.requireNonNull(project, "project"),
                Objects.requireNonNull(types, "types"),
                Objects.requireNonNull(publishedContentRoot, "publishedContentRoot"),
                Spatial3dResourceLoaders.all());
        return new ProductSceneViewResourceResolver(provider);
    }

    @Override
    public Lease<BufferGeometry> acquireMesh(ResourceReference reference) {
        RuntimeResourceLease<Mesh3dResource> lease =
                resources.acquire(Objects.requireNonNull(reference, "reference"), Mesh3dResource.class);
        return new ProductLease<>(lease.value().geometry(), lease);
    }

    @Override
    public Lease<Material> acquireMaterial(ResourceReference reference) {
        RuntimeResourceLease<Material3dResource> lease =
                resources.acquire(Objects.requireNonNull(reference, "reference"), Material3dResource.class);
        return new ProductLease<>(lease.value().material(), lease);
    }

    /** Adapts the existing idempotent product resource retention without transferring value ownership. */
    private record ProductLease<T>(T value, RuntimeResourceLease<?> delegate) implements Lease<T> {
        private ProductLease {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(delegate, "delegate");
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
