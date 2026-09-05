/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Verifies LWJGL framebuffer adaptation without creating native graphics resources. */
final class LwjglScene3dRenderHostTest {
    /** Updates the camera projection and submits a frame for a drawable framebuffer. */
    @Test
    void rendersDrawableFramebuffer() {
        Scene scene = new Scene();
        PerspectiveCamera camera = new PerspectiveCamera(1.0F, 1.0F, 0.1F, 100.0F);
        AtomicReference<Scene> renderedScene = new AtomicReference<>();
        AtomicReference<PerspectiveCamera> renderedCamera = new AtomicReference<>();
        LwjglScene3dRenderHost host = new LwjglScene3dRenderHost(() -> 1600, () -> 900, (value, view) -> {
            renderedScene.set(value);
            renderedCamera.set(view);
        });

        host.render(scene, camera);

        assertThat(camera.aspectRatio()).isEqualTo(16.0F / 9.0F);
        assertThat(renderedScene).hasValue(scene);
        assertThat(renderedCamera).hasValue(camera);
    }

    /** Skips submission while either framebuffer dimension is not drawable. */
    @Test
    void skipsNonDrawableFramebuffer() {
        AtomicInteger submissions = new AtomicInteger();
        PerspectiveCamera camera = new PerspectiveCamera(1.0F, 1.0F, 0.1F, 100.0F);
        Scene scene = new Scene();
        LwjglScene3dRenderHost zeroWidth = new LwjglScene3dRenderHost(() -> 0, () -> 900, (value, view) -> {
            submissions.incrementAndGet();
        });
        LwjglScene3dRenderHost zeroHeight = new LwjglScene3dRenderHost(() -> 1600, () -> 0, (value, view) -> {
            submissions.incrementAndGet();
        });

        zeroWidth.render(scene, camera);
        zeroHeight.render(scene, camera);

        assertThat(submissions).hasValue(0);
        assertThat(camera.aspectRatio()).isEqualTo(1.0F);
    }

    /** Rejects an absent production window before attempting native access. */
    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsAbsentWindow() {
        assertThatThrownBy(() -> new LwjglScene3dRenderHost(null, null)).isInstanceOf(NullPointerException.class);
    }
}
