/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Prototype seam supplying the context and presentation surface used by one renderer. */
interface RendererContext {
    /** Makes this context current for subsequent OpenGL calls. */
    void makeCurrent();

    /** Binds the framebuffer that receives the completed presentation frame. */
    void bindPresentationFramebuffer();

    /** Returns the current presentation framebuffer width in physical pixels. */
    int framebufferWidth();

    /** Returns the current presentation framebuffer height in physical pixels. */
    int framebufferHeight();

    /** Returns the current surface width in logical coordinates. */
    int logicalWidth();

    /** Returns the current surface height in logical coordinates. */
    int logicalHeight();

    /** Releases the renderer's exclusive claim without destroying the owning surface. */
    void release();
}
