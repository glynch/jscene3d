/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.iosurface.macos;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

import io.github.glynch.jscene3d.render.RenderSurface;
import io.github.glynch.jscene3d.render.RenderSurfaceSize;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

/** Exclusive JScene3D renderer access to a host-owned IOSurface and hidden OpenGL context. */
public final class IOSurfaceRenderSurface implements RenderSurface {
    private final Thread renderThread;
    private final long context;
    private final GLCapabilities capabilities;

    private IOSurfaceTarget target;
    private LinearSrgbPresenter presenter;
    private RenderSurfaceSize currentSize;
    private boolean released;

    /**
     * Takes ownership of the initial IOSurface reference on the current OpenGL context thread.
     *
     * @param initialSurface initial acquired native surface and its physical size
     */
    public IOSurfaceRenderSurface(IOSurfaceDescriptor initialSurface) {
        renderThread = Thread.currentThread();
        context = glfwGetCurrentContext();
        if (context == 0L) {
            IOSurfaceBridge.release(initialSurface.handle());
            throw new IllegalStateException("IOSurface render surface requires a current OpenGL context");
        }
        try {
            capabilities = GL.getCapabilities();
        } catch (RuntimeException failure) {
            IOSurfaceBridge.release(initialSurface.handle());
            throw failure;
        }
        IOSurfaceTarget initialTarget = IOSurfaceTarget.create(initialSurface);
        LinearSrgbPresenter initialPresenter = null;
        try {
            initialPresenter = LinearSrgbPresenter.create();
            initialPresenter.resize(initialSurface.width(), initialSurface.height());
            target = initialTarget;
            presenter = initialPresenter;
            currentSize = sizeOf(initialSurface);
        } catch (RuntimeException failure) {
            if (initialPresenter != null) {
                initialPresenter.close();
            }
            initialTarget.close();
            throw failure;
        }
    }

    /** Makes the host context current and rebinds the linear renderer target on every activation. */
    @Override
    public void activate() {
        requireActiveThread();
        if (glfwGetCurrentContext() != context) {
            glfwMakeContextCurrent(context);
        }
        GL.setCapabilities(capabilities);
        presenter.bindLinearFramebuffer();
    }

    /** Reports the current physical size; this proof scene has no logical-coordinate overlay. */
    @Override
    public RenderSurfaceSize size() {
        requireActiveThread();
        return currentSize;
    }

    /** Encodes the completed linear scene frame into the IOSurface-backed framebuffer. */
    public void present() {
        requireActiveThread();
        presenter.present(target.framebuffer);
    }

    /**
     * Atomically adopts a replacement IOSurface without recreating the context or renderer.
     *
     * @param replacement newly acquired surface whose reference ownership transfers here
     */
    public void replaceSurface(IOSurfaceDescriptor replacement) {
        requireActiveThread();
        IOSurfaceTarget nextTarget = IOSurfaceTarget.create(replacement);
        LinearSrgbPresenter nextPresenter = null;
        try {
            nextPresenter = LinearSrgbPresenter.create();
            nextPresenter.resize(replacement.width(), replacement.height());
        } catch (RuntimeException failure) {
            if (nextPresenter != null) {
                nextPresenter.close();
            }
            nextTarget.close();
            throw failure;
        }

        IOSurfaceTarget previousTarget = target;
        LinearSrgbPresenter previousPresenter = presenter;
        target = nextTarget;
        presenter = nextPresenter;
        currentSize = sizeOf(replacement);
        previousPresenter.close();
        previousTarget.close();
    }

    /** Releases renderer access and this adapter's GL and IOSurface resources, not the host context. */
    @Override
    public void release() {
        if (released) {
            return;
        }
        requireActiveThread();
        try {
            presenter.close();
        } finally {
            target.close();
            released = true;
        }
    }

    private void requireActiveThread() {
        if (Thread.currentThread() != renderThread) {
            throw new IllegalStateException("IOSurface render surface accessed from the wrong thread");
        }
        if (released) {
            throw new IllegalStateException("IOSurface render surface was released");
        }
    }

    private static RenderSurfaceSize sizeOf(IOSurfaceDescriptor surface) {
        return new RenderSurfaceSize(surface.width(), surface.height(), surface.width(), surface.height());
    }

    /** One owned IOSurface reference and its OpenGL rectangle texture/FBO attachment. */
    private static final class IOSurfaceTarget {
        private final long surface;
        private final int texture;
        private final int framebuffer;

        private IOSurfaceTarget(long surface, int texture, int framebuffer) {
            this.surface = surface;
            this.texture = texture;
            this.framebuffer = framebuffer;
        }

        static IOSurfaceTarget create(IOSurfaceDescriptor descriptor) {
            long surface = descriptor.handle();
            int texture = 0;
            int framebuffer = 0;
            try {
                if (surface == 0L || descriptor.width() <= 0 || descriptor.height() <= 0) {
                    throw new IllegalArgumentException(
                            "IOSurface descriptor must have a handle and positive dimensions");
                }
                texture = glGenTextures();
                glBindTexture(GL_TEXTURE_RECTANGLE_ARB, texture);
                IOSurfaceBridge.bindToTexture(surface, descriptor.width(), descriptor.height());
                int error = glGetError();
                if (error != GL_NO_ERROR) {
                    throw new IllegalStateException("IOSurface texture bind failed: 0x" + Integer.toHexString(error));
                }
                framebuffer = glGenFramebuffers();
                glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_RECTANGLE_ARB, texture, 0);
                int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    throw new IllegalStateException(
                            "IOSurface framebuffer is incomplete: 0x" + Integer.toHexString(status));
                }
                return new IOSurfaceTarget(surface, texture, framebuffer);
            } catch (RuntimeException failure) {
                if (framebuffer != 0) {
                    glDeleteFramebuffers(framebuffer);
                }
                if (texture != 0) {
                    glDeleteTextures(texture);
                }
                if (surface != 0L) {
                    IOSurfaceBridge.release(surface);
                }
                throw failure;
            } finally {
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glBindTexture(GL_TEXTURE_RECTANGLE_ARB, 0);
            }
        }

        void close() {
            glDeleteFramebuffers(framebuffer);
            glDeleteTextures(texture);
            IOSurfaceBridge.release(surface);
        }
    }
}
