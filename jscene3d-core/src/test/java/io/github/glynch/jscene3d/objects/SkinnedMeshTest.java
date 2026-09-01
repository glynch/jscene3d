/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import java.util.List;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

final class SkinnedMeshTest {
    @Test
    void retainsSkeletonAndDisablesUnreliableStaticBounds() {
        Skeleton skeleton = skeleton();
        try (BufferGeometry geometry = geometry(true);
                StandardMaterial material = new StandardMaterial()) {
            SkinnedMesh mesh = new SkinnedMesh(geometry, material, skeleton);

            assertThat(mesh.skeleton()).isSameAs(skeleton);
            assertThat(mesh.isFrustumCullingEnabled()).isFalse();
        }
    }

    @Test
    void rejectsGeometryWithoutCompleteSkinningAttributes() {
        Skeleton skeleton = skeleton();
        try (BufferGeometry geometry = geometry(false);
                StandardMaterial material = new StandardMaterial()) {
            assertThatIllegalArgumentException().isThrownBy(() -> new SkinnedMesh(geometry, material, skeleton));
        }
    }

    @Test
    void preservesSkinningInvariantWhenGeometryIsReplaced() {
        try (BufferGeometry initial = geometry(true);
                BufferGeometry replacement = geometry(false);
                StandardMaterial material = new StandardMaterial()) {
            SkinnedMesh mesh = new SkinnedMesh(initial, material, skeleton());

            assertThatIllegalArgumentException().isThrownBy(() -> mesh.setGeometry(replacement));
            assertThat(mesh.geometry()).isSameAs(initial);
        }
    }

    private static Skeleton skeleton() {
        return new Skeleton(List.of(new Bone()), List.of(new Matrix4f()));
    }

    private static BufferGeometry geometry(boolean includeWeights) {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(BufferGeometry.POSITION, BufferAttribute.of(new float[9], 3));
        geometry.setAttribute(BufferGeometry.JOINTS, BufferAttribute.of(new float[12], 4));
        if (includeWeights) {
            geometry.setAttribute(
                    BufferGeometry.WEIGHTS,
                    BufferAttribute.of(
                            new float[] {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f}, 4));
        }
        return geometry;
    }
}
