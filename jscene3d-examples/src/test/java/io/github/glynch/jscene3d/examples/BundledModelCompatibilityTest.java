/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.gltf.GltfLoader;
import io.github.glynch.jscene3d.gltf.LoadedGltf;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.objects.Mesh;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies that bundled showcase models remain inside the supported glTF capability profile. */
final class BundledModelCompatibilityTest {
    private static final String WATER_BOTTLE_RESOURCE =
            "/io/github/glynch/jscene3d/examples/water-bottle/WaterBottle.glb";
    private static final String BOOM_BOX_RESOURCE = "/io/github/glynch/jscene3d/examples/boom-box/BoomBox.glb";

    /** Ensures Water Bottle exercises every currently supported StandardMaterial texture role. */
    @Test
    void loadsWaterBottleMaterialMaps() {
        try (LoadedGltf loaded = GltfLoader.load(path(getClass(), WATER_BOTTLE_RESOURCE))) {
            List<Mesh> meshes = new ArrayList<>();
            loaded.scene().traverse(object -> {
                if (object instanceof Mesh mesh) {
                    meshes.add(mesh);
                }
            });

            assertThat(meshes).singleElement().satisfies(mesh -> {
                assertThat(mesh.material()).isInstanceOf(StandardMaterial.class);
                StandardMaterial material = (StandardMaterial) mesh.material();
                assertThat(material.colorMap()).isPresent();
                assertThat(material.metalnessRoughnessMap()).isPresent();
                assertThat(material.normalMap()).isPresent();
                assertThat(material.occlusionMap()).isPresent();
                assertThat(material.emissiveMap()).isPresent();
            });
        }
    }

    /** Ensures Boom Box preserves its metallic, occlusion, normal, and glowing-panel material maps. */
    @Test
    void loadsBoomBoxMaterialMaps() {
        try (LoadedGltf loaded = GltfLoader.load(path(getClass(), BOOM_BOX_RESOURCE))) {
            List<Mesh> meshes = new ArrayList<>();
            loaded.scene().traverse(object -> {
                if (object instanceof Mesh mesh) {
                    meshes.add(mesh);
                }
            });

            assertThat(meshes).singleElement().satisfies(mesh -> {
                assertThat(mesh.material()).isInstanceOf(StandardMaterial.class);
                StandardMaterial material = (StandardMaterial) mesh.material();
                assertThat(material.colorMap()).isPresent();
                assertThat(material.metalnessRoughnessMap()).isPresent();
                assertThat(material.normalMap()).isPresent();
                assertThat(material.occlusionMap()).isPresent();
                assertThat(material.emissiveMap()).isPresent();
            });
        }
    }
}
