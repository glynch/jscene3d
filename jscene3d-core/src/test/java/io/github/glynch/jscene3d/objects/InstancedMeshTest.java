/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.BoundingSphere;
import io.github.glynch.jscene3d.math.Color;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

final class InstancedMeshTest {
    @Test
    void ownsFixedCapacityWithIdentityTransformsAndMutableCount() {
        try (var geometry = BoxGeometry.create(2.0f, 2.0f, 2.0f);
                var material = new BasicMaterial()) {
            InstancedMesh mesh = new InstancedMesh(geometry, material, 3);

            assertThat(mesh.capacity()).isEqualTo(3);
            assertThat(mesh.count()).isEqualTo(3);
            assertThat(mesh.matrixAt(1, new Matrix4f())).isEqualTo(new Matrix4f());

            mesh.setCount(0);
            assertThat(mesh.count()).isZero();
            assertThat(mesh.boundingSphere()).isNull();
            assertThatIllegalArgumentException().isThrownBy(() -> mesh.setCount(4));
        }
    }

    @Test
    void copiesValidatedTransformsAndComputesAggregateBounds() {
        try (var geometry = BoxGeometry.create(2.0f, 2.0f, 2.0f);
                var material = new BasicMaterial()) {
            InstancedMesh mesh = new InstancedMesh(geometry, material, 2);
            Matrix4f first = new Matrix4f().translation(-4.0f, 0.0f, 0.0f);
            Matrix4f second = new Matrix4f().translation(4.0f, 0.0f, 0.0f).scale(2.0f);

            mesh.setMatrixAt(0, first);
            mesh.setMatrixAt(1, second);
            long version = mesh.matrixVersion();
            mesh.setMatrixAt(1, second);
            first.identity();

            assertThat(mesh.matrixAt(0, new Matrix4f()).m30()).isEqualTo(-4.0f);
            assertThat(mesh.matrixVersion()).isEqualTo(version);
            BoundingSphere bounds = mesh.boundingSphere();
            assertThat(bounds).isNotNull();
            assertThat(bounds.radius()).isGreaterThan(5.0f);
            assertThatIllegalArgumentException().isThrownBy(() -> mesh.setMatrixAt(0, new Matrix4f().scale(0.0f)));
        }
    }

    @Test
    void createsWhiteColorStorageLazilyAndCopiesAssignedColors() {
        try (var geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                var material = new BasicMaterial()) {
            InstancedMesh mesh = new InstancedMesh(geometry, material, 2);

            assertThat(mesh.hasInstanceColors()).isFalse();
            assertThatIllegalStateException().isThrownBy(() -> mesh.colorAt(0));

            mesh.setColorAt(1, Color.RED);
            assertThat(mesh.hasInstanceColors()).isTrue();
            assertThat(mesh.colorAt(0)).isEqualTo(Color.WHITE);
            assertThat(mesh.colorAt(1)).isEqualTo(Color.RED);

            mesh.clearInstanceColors();
            assertThat(mesh.hasInstanceColors()).isFalse();
        }
    }

    @Test
    void expandsSharedMorphWeightsOnlyWhenAnInstanceDiverges() {
        try (var geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                var material = new BasicMaterial()) {
            geometry.addMorphTarget(
                    new MorphTarget("inflate", BufferAttribute.of(new float[geometry.vertexCount() * 3], 3), null));
            InstancedMesh mesh = new InstancedMesh(geometry, material, 2);
            mesh.setMorphTargetInfluence(0, 0.25f);

            assertThat(mesh.hasInstanceMorphTargetInfluences()).isFalse();
            assertThat(mesh.morphTargetInfluenceAt(1, 0)).isEqualTo(0.25f);

            mesh.setMorphTargetInfluenceAt(1, 0, 0.8f);

            assertThat(mesh.hasInstanceMorphTargetInfluences()).isTrue();
            assertThat(mesh.morphTargetInfluenceAt(0, 0)).isEqualTo(0.25f);
            assertThat(mesh.morphTargetInfluenceAt(1, 0)).isEqualTo(0.8f);
        }
    }
}
