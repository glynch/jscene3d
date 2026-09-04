/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl.internal;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.Objects;

/** Production render host backed by one caller-owned LWJGL window and renderer. */
public final class LwjglScene3dRenderHost implements Scene3dRenderHost {
    private final Window window;
    private final Renderer renderer;

    /**
     * Creates a render host without taking ownership of its arguments.
     *
     * @param window open render window
     * @param renderer open renderer bound to {@code window}
     */
    public LwjglScene3dRenderHost(Window window, Renderer renderer) {
        this.window = Objects.requireNonNull(window, "window");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public void render(Scene scene, PerspectiveCamera camera) {
        if (window.framebufferWidth() <= 0 || window.framebufferHeight() <= 0) {
            return;
        }
        camera.setAspectRatio(window.framebufferAspectRatio());
        renderer.render(scene, camera);
    }
}
