/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static io.github.glynch.jscene3d.examples.framework.BundledResources.path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.gltf.GltfLoader;
import io.github.glynch.jscene3d.gltf.LoadedGltf;
import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.objects.SkinnedMesh;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;

final class LittlestTokyoAssetTest {
    private static final String MODEL_RESOURCE = "/io/github/glynch/jscene3d/examples/littlest-tokyo/LittlestTokyo.glb";

    @Test
    void loadsBundledDracoMeshSkeletonAndAnimation() {
        try (LoadedGltf loaded =
                GltfLoader.load(path(LittlestTokyoAssetTest.class.getResource(MODEL_RESOURCE), MODEL_RESOURCE))) {
            SkinnedMesh skinnedMesh = firstSkinnedMesh(loaded.scene());

            assertThat(loaded.animations()).singleElement().satisfies(clip -> {
                assertThat(clip.name()).isEqualTo("Take 001");
                assertThat(clip.duration()).isGreaterThan(9.0f);
                assertThat(clip.tracks()).hasSize(96);
            });
            assertThat(skinnedMesh.skeleton().jointCount()).isEqualTo(32);
            assertThat(skinnedMesh.geometry().vertexCount()).isPositive();
        }
    }

    @Test
    void restoresAllDracoPositionOffsetsForTheBundledTrolley() {
        try (LoadedGltf loaded =
                GltfLoader.load(path(LittlestTokyoAssetTest.class.getResource(MODEL_RESOURCE), MODEL_RESOURCE))) {
            Object3D trolley = childAtPath(loaded.scene(), 0, 0, 0, 0, 0, 0, 20);
            BoundingBox bounds = firstMesh(trolley).geometry().computeBoundingBox();

            assertThat(bounds.minimum().x()).isCloseTo(-269.61096f, within(0.01f));
            assertThat(bounds.minimum().y()).isCloseTo(-275.83316f, within(0.01f));
            assertThat(bounds.minimum().z()).isCloseTo(-205.60886f, within(0.01f));
            assertThat(bounds.maximum().x()).isCloseTo(270.20148f, within(0.01f));
            assertThat(bounds.maximum().y()).isCloseTo(274.42902f, within(0.01f));
            assertThat(bounds.maximum().z()).isCloseTo(209.12997f, within(0.01f));
        }
    }

    /** Resolves a stable source-order path in the bundled glTF hierarchy. */
    private static Object3D childAtPath(Object3D root, int... childIndices) {
        Object3D current = root;
        for (int childIndex : childIndices) {
            current = current.children().get(childIndex);
        }
        return current;
    }

    /** Returns the first mesh below one bundled source node. */
    private static Mesh firstMesh(Object3D root) {
        Deque<Object3D> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            Object3D object = pending.removeFirst();
            if (object instanceof Mesh mesh) {
                return mesh;
            }
            pending.addAll(object.children());
        }
        throw new AssertionError("Bundled Littlest Tokyo node has no Mesh");
    }

    private static SkinnedMesh firstSkinnedMesh(Object3D root) {
        Deque<Object3D> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            Object3D object = pending.removeFirst();
            if (object instanceof SkinnedMesh skinnedMesh) {
                return skinnedMesh;
            }
            pending.addAll(object.children());
        }
        throw new AssertionError("Bundled Littlest Tokyo scene has no SkinnedMesh");
    }
}
