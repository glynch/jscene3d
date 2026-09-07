/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import io.github.glynch.jscene3d.lwjgl.internal.WindowContextRegistry;
import io.github.glynch.jscene3d.platform.Window;

/** Adapts an exclusively claimed JScene3D window to the renderer's prototype context seam. */
final class WindowRendererContext implements RendererContext {
    private final Window window;
    private final WindowContextRegistry.Access access;

    /** Stores the claimed window and its renderer-only access token. */
    private WindowRendererContext(Window window, WindowContextRegistry.Access access) {
        this.window = window;
        this.access = access;
    }

    /** Claims one open window and returns its renderer context. */
    static WindowRendererContext claim(Window window) {
        return new WindowRendererContext(window, WindowContextRegistry.claim(window));
    }

    @Override
    public void makeCurrent() {
        access.makeCurrent();
    }

    @Override
    public void bindPresentationFramebuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public int framebufferWidth() {
        return access.framebufferWidth();
    }

    @Override
    public int framebufferHeight() {
        return access.framebufferHeight();
    }

    @Override
    public int logicalWidth() {
        return window.width();
    }

    @Override
    public int logicalHeight() {
        return window.height();
    }

    @Override
    public void release() {
        WindowContextRegistry.release(window, access);
    }
}
