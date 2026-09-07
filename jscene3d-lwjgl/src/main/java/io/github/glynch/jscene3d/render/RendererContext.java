/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/**
 * Prototype seam supplying the context and presentation surface used by one renderer.
 *
 * <p>This type is public only so the throwaway JavaFX/OpenGLFX prototype can test a named-module
 * boundary. It is not an accepted renderer-host API and must not be merged from the prototype
 * branch in its current form.
 */
public interface RendererContext {
    /** Makes this context current for subsequent OpenGL calls. */
    void makeCurrent();

    /** Binds the framebuffer that receives the completed presentation frame. */
    void bindPresentationFramebuffer();

    /**
     * Returns the current presentation framebuffer width in physical pixels.
     *
     * @return framebuffer width in physical pixels
     */
    int framebufferWidth();

    /**
     * Returns the current presentation framebuffer height in physical pixels.
     *
     * @return framebuffer height in physical pixels
     */
    int framebufferHeight();

    /**
     * Returns the current surface width in logical coordinates.
     *
     * @return surface width in logical coordinates
     */
    int logicalWidth();

    /**
     * Returns the current surface height in logical coordinates.
     *
     * @return surface height in logical coordinates
     */
    int logicalHeight();

    /** Releases the renderer's exclusive claim without destroying the owning surface. */
    void release();
}
