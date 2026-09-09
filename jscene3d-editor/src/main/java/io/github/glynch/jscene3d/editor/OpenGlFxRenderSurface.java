/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.canvas.events.GLRenderEvent;
import io.github.glynch.jscene3d.render.RenderSurface;
import io.github.glynch.jscene3d.render.RenderSurfaceSize;
import org.jspecify.annotations.Nullable;

/** Adapts an OpenGLFX callback context and presentation framebuffer to JScene3D. */
final class OpenGlFxRenderSurface implements RenderSurface {
    private RenderSurfaceSize currentSize = new RenderSurfaceSize(0, 0, 0, 0);
    private @Nullable Thread contextThread;
    private @Nullable SrgbPresentationFramebuffer presentation;
    private int presentationFramebuffer;
    private boolean released;

    /** Captures the framebuffer and dimensions supplied for one OpenGLFX render callback. */
    void beginFrame(GLCanvas canvas, GLRenderEvent event) {
        requireAvailable();
        requireContextThread();
        presentationFramebuffer = event.fbo;
        int framebufferWidth = Math.max(event.width, 0);
        int framebufferHeight = Math.max(event.height, 0);
        int logicalWidth = Math.max((int) Math.ceil(canvas.getWidth()), 0);
        int logicalHeight = Math.max((int) Math.ceil(canvas.getHeight()), 0);
        if (dimensionsChanged(framebufferWidth, framebufferHeight, logicalWidth, logicalHeight)) {
            currentSize = new RenderSurfaceSize(framebufferWidth, framebufferHeight, logicalWidth, logicalHeight);
            requirePresentation().resize(framebufferWidth, framebufferHeight);
        }
    }

    /** Makes the callback's presentation framebuffer current for renderer output. */
    @Override
    public void activate() {
        requireAvailable();
        requireContextThread();
        requirePresentation().bindLinearFramebuffer();
    }

    /** Returns the dimensions captured from the current or most recent callback. */
    @Override
    public RenderSurfaceSize size() {
        requireAvailable();
        requireContextThread();
        return currentSize;
    }

    /** Ends renderer access without disposing the OpenGLFX canvas or its context. */
    @Override
    public void release() {
        requireAvailable();
        requireContextThread();
        SrgbPresentationFramebuffer current = presentation;
        presentation = null;
        if (current != null) {
            current.close();
        }
        released = true;
    }

    /** Converts the completed linear frame to sRGB in OpenGLFX's presentation framebuffer. */
    void present() {
        requireAvailable();
        requireContextThread();
        requirePresentation().present(presentationFramebuffer);
    }

    /** Returns whether any physical or logical dimension changed. */
    private boolean dimensionsChanged(
            int framebufferWidth, int framebufferHeight, int logicalWidth, int logicalHeight) {
        return currentSize.framebufferWidth() != framebufferWidth
                || currentSize.framebufferHeight() != framebufferHeight
                || currentSize.logicalWidth() != logicalWidth
                || currentSize.logicalHeight() != logicalHeight;
    }

    /** Lazily creates color-conversion resources in the active OpenGLFX context. */
    private SrgbPresentationFramebuffer requirePresentation() {
        SrgbPresentationFramebuffer current = presentation;
        if (current == null) {
            current = SrgbPresentationFramebuffer.create();
            presentation = current;
        }
        return current;
    }

    /** Claims the first callback thread and rejects access from every other thread. */
    private void requireContextThread() {
        Thread currentThread = Thread.currentThread();
        Thread owner = contextThread;
        if (owner == null) {
            contextThread = currentThread;
        } else if (owner != currentThread) {
            throw new IllegalStateException("OpenGLFX render surface accessed from the wrong thread");
        }
    }

    /** Rejects access after the renderer has released the surface. */
    private void requireAvailable() {
        if (released) {
            throw new IllegalStateException("OpenGLFX render surface has been released");
        }
    }
}
