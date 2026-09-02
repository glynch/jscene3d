/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.gltf.GltfLoader;
import io.github.glynch.jscene3d.gltf.LoadedGltf;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;

/** Verifies the bundled morph example asset and the loader capabilities it exercises. */
final class MorphStressTestAssetTest {
    private static final String MODEL_RESOURCE =
            "/io/github/glynch/jscene3d/examples/morph-stress-test/MorphStressTest.glb";

    @Test
    void loadsEightMorphTargetsAndAllWeightAnimations() {
        try (LoadedGltf loaded =
                GltfLoader.load(path(MorphStressTestAssetTest.class.getResource(MODEL_RESOURCE), MODEL_RESOURCE))) {
            Mesh mesh = firstMorphMesh(loaded.scene());

            assertThat(mesh.morphTargetCount()).isEqualTo(8);
            assertThat(mesh.geometry().morphTargets())
                    .allSatisfy(target -> assertThat(target.normals()).isPresent());
            assertThat(loaded.animations())
                    .extracting(clip -> clip.name())
                    .containsExactlyInAnyOrder("Individuals", "TheWave", "Pulse");
            assertThat(loaded.animations())
                    .allSatisfy(clip -> assertThat(clip.tracks()).hasSize(2));
        }
    }

    /** Finds the first mesh carrying imported morph targets. */
    private static Mesh firstMorphMesh(Object3D root) {
        Deque<Object3D> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            Object3D object = pending.removeFirst();
            if (object instanceof Mesh mesh && mesh.morphTargetCount() > 0) {
                return mesh;
            }
            pending.addAll(object.children());
        }
        throw new AssertionError("Bundled Morph Stress Test scene has no morph-target Mesh");
    }
}
