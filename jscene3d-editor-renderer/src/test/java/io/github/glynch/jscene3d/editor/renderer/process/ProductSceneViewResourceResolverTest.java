/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.spatial3d.Material3dResource;
import io.github.glynch.jscene3d.project.spatial3d.Mesh3dResource;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.textures.Texture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifies adaptation of fixed product-owned spatial resources into the Scene View boundary. */
final class ProductSceneViewResourceResolverTest {
    private static final ResourceReference MESH = ResourceReference.asset("mesh");
    private static final ResourceReference MATERIAL = ResourceReference.asset("material");

    @Test
    void exposesSharedGeometryMaterialAndNestedTextureOnlyWhileLeased() {
        ProductResources resources = new ProductResources();
        ProductSceneViewResourceResolver resolver = new ProductSceneViewResourceResolver(resources);

        SceneViewResourceResolver.Lease<BufferGeometry> mesh = resolver.acquireMesh(MESH);
        SceneViewResourceResolver.Lease<Material> material = resolver.acquireMaterial(MATERIAL);

        assertThat(mesh.value()).isSameAs(resources.geometry);
        assertThat(material.value()).isSameAs(resources.material);
        assertThat(((BasicMaterial) material.value()).colorMap()).contains(resources.texture);
        assertThat(resources.releases).hasValue(0);

        material.close();
        mesh.close();
        material.close();
        mesh.close();

        assertThat(resources.releases).hasValue(2);
        assertThat(resources.materialResource.isClosed()).isTrue();
        assertThat(resources.meshResource.isClosed()).isTrue();
        assertThat(resources.texture.isClosed()).isTrue();
    }

    /** Fixed test provider that models product ownership of nested texture data. */
    private static final class ProductResources implements RuntimeResourceProvider {
        private final BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
        private final Texture texture = Texture.baseColor(1, 1, new byte[] {0, 0, 0, (byte) 255});
        private final BasicMaterial material = new BasicMaterial();
        private final Mesh3dResource meshResource = Mesh3dResource.owning(geometry);
        private final Material3dResource materialResource = Material3dResource.owning(material);
        private final AtomicInteger releases = new AtomicInteger();

        private ProductResources() {
            material.setColorMap(texture);
        }

        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            if (reference.equals(MESH) && valueType.equals(Mesh3dResource.class)) {
                return lease(valueType.cast(meshResource), () -> {
                    meshResource.close();
                    releases.incrementAndGet();
                });
            }
            if (reference.equals(MATERIAL) && valueType.equals(Material3dResource.class)) {
                return lease(valueType.cast(materialResource), () -> {
                    materialResource.close();
                    texture.close();
                    releases.incrementAndGet();
                });
            }
            throw new IllegalStateException("unexpected resource acquisition");
        }

        private static <T> RuntimeResourceLease<T> lease(T value, Runnable release) {
            return RuntimeResourceLease.of(value, release);
        }
    }
}
