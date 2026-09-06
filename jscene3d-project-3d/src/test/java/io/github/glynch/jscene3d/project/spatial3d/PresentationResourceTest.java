/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

/** Verifies ownership and terminal state of immutable-use presentation resources. */
final class PresentationResourceTest {
    /** Closes adopted backend resources exactly once and rejects later internal access. */
    @Test
    void closesAdoptedResources() {
        Mesh3dResource mesh = Mesh3dResource.owning(BoxGeometry.create(1.0F, 2.0F, 3.0F));
        Material3dResource material = Material3dResource.owning(new LambertMaterial(Color.BLUE));

        assertThat(mesh.geometry().isClosed()).isFalse();
        assertThat(material.material().isClosed()).isFalse();

        mesh.close();
        mesh.close();
        material.close();
        material.close();

        assertThat(mesh.isClosed()).isTrue();
        assertThat(material.isClosed()).isTrue();
        assertThatThrownBy(mesh::geometry)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
        assertThatThrownBy(material::material)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    /** Rejects backend resources that have already reached terminal state. */
    @Test
    void rejectsClosedBackendResources() {
        var geometry = BoxGeometry.create(1.0F, 1.0F, 1.0F);
        var material = new LambertMaterial(Color.WHITE);
        geometry.close();
        material.close();

        assertThatThrownBy(() -> Mesh3dResource.owning(geometry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("open");
        assertThatThrownBy(() -> Material3dResource.owning(material))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("open");
    }
}
