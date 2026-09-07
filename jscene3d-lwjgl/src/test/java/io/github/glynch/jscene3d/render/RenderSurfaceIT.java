/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.scenes.Scene;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

final class RenderSurfaceIT {
    private static final int SURFACE_SIZE = 64;

    @Test
    void presentsToneMappedFramesIntoAHostFramebufferAndReleasesOnlyAccess() {
        try (Window window = Window.create("External render surface integration test")) {
            FramebufferRenderSurface surface = FramebufferRenderSurface.create(window);
            RendererOptions options = RendererOptions.builder()
                    .toneMapping(ToneMapping.ACES_FILMIC)
                    .build();
            Renderer renderer = Renderer.create(surface, options);
            try {
                Scene scene = new Scene();
                scene.setBackground(Color.RED);
                PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 10.0f);

                renderer.render(scene, camera);

                assertThat(surface.centerPixelRed()).isGreaterThan(0);
                assertThat(surface.activationCount()).isGreaterThanOrEqualTo(4);
            } finally {
                renderer.close();
            }

            assertThat(surface.releaseCount()).isOne();
            assertThat(window.isClosed()).isFalse();
        }
    }

    /** Non-default framebuffer adapter representing an embedded host control. */
    private static final class FramebufferRenderSurface implements RenderSurface {
        private final WindowRenderSurface windowSurface;
        private final int framebuffer;
        private final int colorTexture;
        private final RenderSurfaceSize size =
                new RenderSurfaceSize(SURFACE_SIZE, SURFACE_SIZE, SURFACE_SIZE, SURFACE_SIZE);

        private int activationCount;
        private int releaseCount;

        /** Stores initialized OpenGL objects and the claimed window access. */
        private FramebufferRenderSurface(WindowRenderSurface windowSurface, int framebuffer, int colorTexture) {
            this.windowSurface = windowSurface;
            this.framebuffer = framebuffer;
            this.colorTexture = colorTexture;
        }

        /** Creates and clears a complete non-default presentation framebuffer. */
        private static FramebufferRenderSurface create(Window window) {
            WindowRenderSurface windowSurface = WindowRenderSurface.claim(window);
            windowSurface.activate();
            int colorTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, colorTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, SURFACE_SIZE, SURFACE_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            int framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                glDeleteFramebuffers(framebuffer);
                glDeleteTextures(colorTexture);
                windowSurface.release();
                throw new IllegalStateException("Cannot create integration-test presentation framebuffer");
            }
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            return new FramebufferRenderSurface(windowSurface, framebuffer, colorTexture);
        }

        @Override
        public void activate() {
            windowSurface.activate();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            activationCount++;
        }

        @Override
        public RenderSurfaceSize size() {
            return size;
        }

        @Override
        public void release() {
            glDeleteFramebuffers(framebuffer);
            glDeleteTextures(colorTexture);
            windowSurface.release();
            releaseCount++;
        }

        /** Reads the unsigned red channel at the presentation framebuffer center. */
        private int centerPixelRed() {
            activate();
            ByteBuffer pixel = ByteBuffer.allocateDirect(4);
            glReadPixels(SURFACE_SIZE / 2, SURFACE_SIZE / 2, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
            return Byte.toUnsignedInt(pixel.get(0));
        }

        /** Returns how often the renderer or test activated this surface. */
        private int activationCount() {
            return activationCount;
        }

        /** Returns how often the renderer released this surface. */
        private int releaseCount() {
            return releaseCount;
        }
    }
}
