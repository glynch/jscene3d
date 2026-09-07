/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/**
 * Exclusive renderer access to a host-owned OpenGL context and presentation framebuffer.
 *
 * <p>A host adapter creates one access object for one {@link Renderer} and transfers that access
 * to {@link Renderer#create(RenderSurface)}. The renderer invokes every method on the calling
 * thread; implementations must enforce any context-thread affinity required by their host.
 *
 * <p>The host continues to own the window, control, native context, and presentation lifecycle.
 * Rendering writes into the framebuffer bound by {@link #activate()}, but does not swap buffers or
 * otherwise publish the completed frame. The host may therefore render overlays or other content
 * before presenting it.
 *
 * <p>The context must support the renderer's OpenGL 3.3 core profile and have LWJGL capabilities
 * available on the calling thread. The presentation framebuffer must be complete and provide the
 * attachments required by the content being rendered. These requirements are validated by the
 * OpenGL implementation when the renderer realizes or draws resources.
 *
 * <p>When renderer construction fails or the renderer closes, it calls {@link #release()} exactly
 * once. Releasing ends the renderer's exclusive access but must not destroy the host-owned surface
 * or context. No other method is invoked after release.
 */
public interface RenderSurface {
    /**
     * Makes the host context current, establishes its LWJGL capabilities, and binds the framebuffer
     * that receives presentation output.
     *
     * <p>The renderer may invoke this more than once during a frame because shadow and
     * tone-mapping passes use renderer-owned framebuffers temporarily.
     *
     * @throws IllegalStateException if called on the wrong thread or after host invalidation
     */
    void activate();

    /**
     * Returns one internally consistent snapshot of the current physical and logical dimensions.
     * Implementations should reuse an unchanged snapshot so normal frame rendering does not require
     * allocation.
     *
     * @return current surface size
     * @throws IllegalStateException if called on the wrong thread or after host invalidation
     */
    RenderSurfaceSize size();

    /**
     * Ends the renderer's exclusive access without destroying the host-owned surface or context.
     *
     * @throws IllegalStateException if the host can no longer release the access
     */
    void release();
}
