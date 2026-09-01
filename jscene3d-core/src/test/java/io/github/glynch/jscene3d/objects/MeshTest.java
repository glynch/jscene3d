/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

final class MeshTest {
    @Test
    void retainsGeometryAndMaterialWithoutOwningTheirLifecycle() {
        try (BufferGeometry geometry = geometry();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            Mesh first = new Mesh(geometry, material);
            Mesh second = new Mesh(geometry, material);

            assertThat(first.geometry()).isSameAs(geometry);
            assertThat(first.material()).isSameAs(material);
            assertThat(second.geometry()).isSameAs(geometry);
            assertThat(second.material()).isSameAs(material);
            assertThat(first.parent()).isNull();
            assertThat(first.isShadowCastingEnabled()).isFalse();
            assertThat(first.isShadowReceivingEnabled()).isFalse();
        }
    }

    @Test
    void controlsShadowParticipationIndependently() {
        try (BufferGeometry geometry = geometry();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);

            mesh.setShadowCastingEnabled(true);
            mesh.setShadowReceivingEnabled(true);

            assertThat(mesh.isShadowCastingEnabled()).isTrue();
            assertThat(mesh.isShadowReceivingEnabled()).isTrue();
        }
    }

    @Test
    void ownsVersionedGeometryOrderedMorphInfluences() {
        try (BufferGeometry geometry = geometry();
                BasicMaterial material = new BasicMaterial()) {
            geometry.addMorphTarget(new MorphTarget("smile", BufferAttribute.of(new float[9], 3), null));
            Mesh mesh = new Mesh(geometry, material);

            assertThat(mesh.morphTargetCount()).isOne();
            assertThat(mesh.morphTargetIndex("smile")).hasValue(0);
            assertThat(mesh.morphTargetInfluence(0)).isZero();

            mesh.setMorphTargetInfluence(0, 0.75f);
            long version = mesh.morphTargetInfluenceVersion();
            mesh.setMorphTargetInfluence(0, 0.75f);

            assertThat(mesh.morphTargetInfluence(0)).isEqualTo(0.75f);
            assertThat(mesh.morphTargetInfluenceVersion()).isEqualTo(version);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> mesh.setMorphTargetInfluences(new float[] {0.0f, 1.0f}));
        }
    }

    @Test
    void replacesGeometryAndMaterialReferences() {
        try (BufferGeometry firstGeometry = geometry();
                BufferGeometry secondGeometry = geometry();
                BasicMaterial firstMaterial = new BasicMaterial();
                BasicMaterial secondMaterial = new BasicMaterial(Color.BLUE)) {
            Mesh mesh = new Mesh(firstGeometry, firstMaterial);

            mesh.setGeometry(secondGeometry);
            mesh.setMaterial(secondMaterial);

            assertThat(mesh.geometry()).isSameAs(secondGeometry);
            assertThat(mesh.material()).isSameAs(secondMaterial);
        }
    }

    @Test
    void rejectsClosedResources() {
        BufferGeometry closedGeometry = geometry();
        try (BasicMaterial material = new BasicMaterial()) {
            closedGeometry.close();
            assertThatIllegalArgumentException().isThrownBy(() -> new Mesh(closedGeometry, material));
        }

        BasicMaterial closedMaterial = new BasicMaterial();
        try (BufferGeometry geometry = geometry()) {
            closedMaterial.close();
            assertThatIllegalArgumentException().isThrownBy(() -> new Mesh(geometry, closedMaterial));
        }
    }

    @Test
    void reportsResourcesClosedAfterBinding() {
        BufferGeometry geometry = geometry();
        BasicMaterial material = new BasicMaterial();
        try {
            Mesh mesh = new Mesh(geometry, material);

            geometry.close();
            assertThatIllegalStateException().isThrownBy(mesh::geometry);

            material.close();
            assertThatIllegalStateException().isThrownBy(mesh::material);
        } finally {
            geometry.close();
            material.close();
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullResources() {
        try (BufferGeometry geometry = geometry();
                BasicMaterial material = new BasicMaterial()) {
            assertThatNullPointerException().isThrownBy(() -> new Mesh(null, material));
            assertThatNullPointerException().isThrownBy(() -> new Mesh(geometry, null));
            Mesh mesh = new Mesh(geometry, material);
            assertThatNullPointerException().isThrownBy(() -> mesh.setGeometry(null));
            assertThatNullPointerException().isThrownBy(() -> mesh.setMaterial(null));
        }
    }

    private static BufferGeometry geometry() {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(BufferGeometry.POSITION, BufferAttribute.of(new float[9], 3));
        return geometry;
    }
}
