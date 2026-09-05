/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.project.runtime.scene3d.Scene3dRenderHost;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

/** Production render host backed by one caller-owned LWJGL window and renderer. */
public final class LwjglScene3dRenderHost implements Scene3dRenderHost {
    private final IntSupplier framebufferWidth;
    private final IntSupplier framebufferHeight;
    private final BiConsumer<Scene, PerspectiveCamera> renderFrame;

    /**
     * Creates a render host without taking ownership of its arguments.
     *
     * @param window open render window
     * @param renderer open renderer bound to {@code window}
     */
    public LwjglScene3dRenderHost(Window window, Renderer renderer) {
        this(window::framebufferWidth, window::framebufferHeight, renderer::render);
    }

    /** Creates a render host over independently testable frame operations. */
    LwjglScene3dRenderHost(
            IntSupplier framebufferWidth,
            IntSupplier framebufferHeight,
            BiConsumer<Scene, PerspectiveCamera> renderFrame) {
        this.framebufferWidth = Objects.requireNonNull(framebufferWidth, "framebufferWidth");
        this.framebufferHeight = Objects.requireNonNull(framebufferHeight, "framebufferHeight");
        this.renderFrame = Objects.requireNonNull(renderFrame, "renderFrame");
    }

    @Override
    public void render(Scene scene, PerspectiveCamera camera) {
        int width = framebufferWidth.getAsInt();
        int height = framebufferHeight.getAsInt();
        if (width <= 0 || height <= 0) {
            return;
        }
        camera.setAspectRatio(width / (float) height);
        renderFrame.accept(scene, camera);
    }
}
