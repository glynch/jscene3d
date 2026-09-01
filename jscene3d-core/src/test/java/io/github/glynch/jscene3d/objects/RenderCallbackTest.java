/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.scenes.Scene;
import org.junit.jupiter.api.Test;

final class RenderCallbackTest {
    @Test
    void configuresAndClearsMainCallbacksForEveryRenderableKind() {
        RenderCallback before = ignored -> {};
        RenderCallback after = ignored -> {};
        try (BufferGeometry meshGeometry = triangle();
                BasicMaterial meshMaterial = new BasicMaterial();
                BufferGeometry lineGeometry = line();
                LineBasicMaterial lineMaterial = new LineBasicMaterial()) {
            Mesh mesh = new Mesh(meshGeometry, meshMaterial);
            LineSegments segments = new LineSegments(lineGeometry, lineMaterial);

            assertThat(mesh.beforeRenderCallback()).isEmpty();
            assertThat(mesh.afterRenderCallback()).isEmpty();
            assertThat(segments.beforeRenderCallback()).isEmpty();
            assertThat(segments.afterRenderCallback()).isEmpty();

            mesh.setBeforeRenderCallback(before);
            mesh.setAfterRenderCallback(after);
            segments.setBeforeRenderCallback(before);
            segments.setAfterRenderCallback(after);

            assertThat(mesh.beforeRenderCallback()).containsSame(before);
            assertThat(mesh.afterRenderCallback()).containsSame(after);
            assertThat(segments.beforeRenderCallback()).containsSame(before);
            assertThat(segments.afterRenderCallback()).containsSame(after);

            mesh.clearBeforeRenderCallback();
            mesh.clearAfterRenderCallback();
            segments.clearBeforeRenderCallback();
            segments.clearAfterRenderCallback();

            assertThat(mesh.beforeRenderCallback()).isEmpty();
            assertThat(mesh.afterRenderCallback()).isEmpty();
            assertThat(segments.beforeRenderCallback()).isEmpty();
            assertThat(segments.afterRenderCallback()).isEmpty();
        }
    }

    @Test
    void configuresAndClearsMeshShadowCallbacks() {
        RenderCallback before = ignored -> {};
        RenderCallback after = ignored -> {};
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);

            mesh.setBeforeShadowRenderCallback(before);
            mesh.setAfterShadowRenderCallback(after);

            assertThat(mesh.beforeShadowRenderCallback()).containsSame(before);
            assertThat(mesh.afterShadowRenderCallback()).containsSame(after);

            mesh.clearBeforeShadowRenderCallback();
            mesh.clearAfterShadowRenderCallback();

            assertThat(mesh.beforeShadowRenderCallback()).isEmpty();
            assertThat(mesh.afterShadowRenderCallback()).isEmpty();
        }
    }

    @Test
    void exposesBackendNeutralDrawContext() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Scene scene = new Scene();
            PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 10.0f);
            Mesh mesh = new Mesh(geometry, material);

            RenderContext context = RenderContext.of(scene, camera, mesh, geometry, material, RenderPass.MAIN);

            assertThat(context.scene()).isSameAs(scene);
            assertThat(context.camera()).isSameAs(camera);
            assertThat(context.object()).isSameAs(mesh);
            assertThat(context.geometry()).isSameAs(geometry);
            assertThat(context.material()).isSameAs(material);
            assertThat(context.pass()).isEqualTo(RenderPass.MAIN);
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsNullCallbacksAndContextValues() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Scene scene = new Scene();
            PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 10.0f);
            Mesh mesh = new Mesh(geometry, material);

            assertThatNullPointerException().isThrownBy(() -> mesh.setBeforeRenderCallback(null));
            assertThatNullPointerException().isThrownBy(() -> mesh.setAfterRenderCallback(null));
            assertThatNullPointerException().isThrownBy(() -> mesh.setBeforeShadowRenderCallback(null));
            assertThatNullPointerException().isThrownBy(() -> mesh.setAfterShadowRenderCallback(null));
            assertThatNullPointerException()
                    .isThrownBy(() -> RenderContext.of(null, camera, mesh, geometry, material, RenderPass.MAIN));
            assertThatNullPointerException()
                    .isThrownBy(() -> RenderContext.of(scene, null, mesh, geometry, material, RenderPass.MAIN));
            assertThatNullPointerException()
                    .isThrownBy(() -> RenderContext.of(scene, camera, null, geometry, material, RenderPass.MAIN));
            assertThatNullPointerException()
                    .isThrownBy(() -> RenderContext.of(scene, camera, mesh, null, material, RenderPass.MAIN));
            assertThatNullPointerException()
                    .isThrownBy(() -> RenderContext.of(scene, camera, mesh, geometry, null, RenderPass.MAIN));
            assertThatNullPointerException()
                    .isThrownBy(() -> RenderContext.of(scene, camera, mesh, geometry, material, null));
        }
    }

    /** Creates one renderable triangle. */
    private static BufferGeometry triangle() {
        return BufferGeometry.builder()
                .positions(-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f)
                .build();
    }

    /** Creates one independent line segment. */
    private static BufferGeometry line() {
        return BufferGeometry.builder()
                .positions(-0.5f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f)
                .build();
    }
}
