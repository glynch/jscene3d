/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.prototype.javafx;

import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.canvas.events.GLRenderEvent;
import io.github.glynch.jscene3d.render.RendererContext;

/** Adapts OpenGLFX's current callback context and swap-chain framebuffer to JScene3D. */
final class OpenGlFxRendererContext implements RendererContext {
    private int presentationFramebuffer;
    private int framebufferWidth = 1;
    private int framebufferHeight = 1;
    private int logicalWidth = 1;
    private int logicalHeight = 1;
    private boolean released;

    /** Captures the framebuffer and dimensions supplied for one OpenGLFX render callback. */
    void beginFrame(GLCanvas canvas, GLRenderEvent event) {
        if (released) {
            throw new IllegalStateException("Renderer context has been released");
        }
        presentationFramebuffer = event.fbo;
        framebufferWidth = Math.max(event.width, 1);
        framebufferHeight = Math.max(event.height, 1);
        logicalWidth = Math.max((int) Math.ceil(canvas.getWidth()), 1);
        logicalHeight = Math.max((int) Math.ceil(canvas.getHeight()), 1);
    }

    @Override
    public void makeCurrent() {
        if (released) {
            throw new IllegalStateException("Renderer context has been released");
        }
        // OpenGLFX guarantees that its context is current throughout each callback.
    }

    @Override
    public void bindPresentationFramebuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, presentationFramebuffer);
    }

    @Override
    public int framebufferWidth() {
        return framebufferWidth;
    }

    @Override
    public int framebufferHeight() {
        return framebufferHeight;
    }

    @Override
    public int logicalWidth() {
        return logicalWidth;
    }

    @Override
    public int logicalHeight() {
        return logicalHeight;
    }

    @Override
    public void release() {
        released = true;
    }
}
