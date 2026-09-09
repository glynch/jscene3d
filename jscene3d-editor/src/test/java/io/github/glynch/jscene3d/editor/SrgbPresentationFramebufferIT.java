/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

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

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

final class SrgbPresentationFramebufferIT {
    private static final int FRAMEBUFFER_SIZE = 64;

    @Test
    void encodesLinearFrameForTheOpenGlFxHostFramebuffer() {
        try (Window window = Window.create("OpenGLFX color presentation integration test");
                Renderer renderer = Renderer.create(window);
                SrgbPresentationFramebuffer presentation = SrgbPresentationFramebuffer.create()) {
            HostFramebuffer host = HostFramebuffer.create();
            try {
                presentation.resize(FRAMEBUFFER_SIZE, FRAMEBUFFER_SIZE);
                presentation.bindLinearFramebuffer();
                Color gray = Color.GRAY;
                glClearColor(gray.red(), gray.green(), gray.blue(), 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);

                presentation.present(host.framebuffer());

                assertThat(host.centerPixelRed()).isBetween(127, 129);
            } finally {
                host.close();
            }
        }
    }

    /** Owns a linear normalized framebuffer representing OpenGLFX's presentation attachment. */
    private static final class HostFramebuffer implements AutoCloseable {
        private final int framebuffer;
        private final int colorTexture;

        /** Stores the complete framebuffer and attached color texture. */
        private HostFramebuffer(int framebuffer, int colorTexture) {
            this.framebuffer = framebuffer;
            this.colorTexture = colorTexture;
        }

        /** Creates a complete linear normalized framebuffer matching OpenGLFX's storage. */
        private static HostFramebuffer create() {
            int colorTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, colorTexture);
            glTexImage2D(
                    GL_TEXTURE_2D, 0, GL_RGBA8, FRAMEBUFFER_SIZE, FRAMEBUFFER_SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            int framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                glDeleteFramebuffers(framebuffer);
                glDeleteTextures(colorTexture);
                throw new IllegalStateException("Cannot create OpenGLFX integration-test host framebuffer");
            }
            return new HostFramebuffer(framebuffer, colorTexture);
        }

        /** Returns the OpenGL name of the framebuffer receiving presentation output. */
        private int framebuffer() {
            return framebuffer;
        }

        /** Reads the unsigned red channel from the center of the host framebuffer. */
        private int centerPixelRed() {
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            ByteBuffer pixel = ByteBuffer.allocateDirect(4);
            glReadPixels(FRAMEBUFFER_SIZE / 2, FRAMEBUFFER_SIZE / 2, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
            return Byte.toUnsignedInt(pixel.get(0));
        }

        /** Deletes the host-owned framebuffer and color texture. */
        @Override
        public void close() {
            glDeleteFramebuffers(framebuffer);
            glDeleteTextures(colorTexture);
        }
    }
}
