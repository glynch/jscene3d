/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import io.github.glynch.jscene3d.lwjgl.internal.WindowContextRegistry;
import io.github.glynch.jscene3d.platform.Window;

/** Adapts an exclusively claimed JScene3D window to the renderer's surface seam. */
final class WindowRenderSurface implements RenderSurface {
    private final Window window;
    private final WindowContextRegistry.Access access;

    private RenderSurfaceSize lastSize;

    /** Stores the claimed window and its renderer-only access token. */
    private WindowRenderSurface(Window window, WindowContextRegistry.Access access) {
        this.window = window;
        this.access = access;
        lastSize = new RenderSurfaceSize(
                access.framebufferWidth(), access.framebufferHeight(), window.width(), window.height());
    }

    /** Claims one open window and returns its renderer surface. */
    static WindowRenderSurface claim(Window window) {
        return new WindowRenderSurface(window, WindowContextRegistry.claim(window));
    }

    @Override
    public void activate() {
        access.makeCurrent();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public RenderSurfaceSize size() {
        int framebufferWidth = access.framebufferWidth();
        int framebufferHeight = access.framebufferHeight();
        int logicalWidth = window.width();
        int logicalHeight = window.height();
        if (lastSize.framebufferWidth() != framebufferWidth
                || lastSize.framebufferHeight() != framebufferHeight
                || lastSize.logicalWidth() != logicalWidth
                || lastSize.logicalHeight() != logicalHeight) {
            lastSize = new RenderSurfaceSize(framebufferWidth, framebufferHeight, logicalWidth, logicalHeight);
        }
        return lastSize;
    }

    @Override
    public void release() {
        WindowContextRegistry.release(window, access);
    }
}
