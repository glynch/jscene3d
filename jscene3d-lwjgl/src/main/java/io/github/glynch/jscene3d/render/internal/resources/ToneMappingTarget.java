/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glBlitFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;
import static org.lwjgl.opengl.GL30.glRenderbufferStorageMultisample;

import io.github.glynch.jscene3d.render.internal.PrimitiveTopology;
import io.github.glynch.jscene3d.render.internal.programs.ToneMappingProgram;

/** Resizable renderer-owned HDR target with optional multisample resolve. */
public final class ToneMappingTarget implements AutoCloseable {
    private final int vertexArray = glGenVertexArrays();

    private int sceneFramebuffer;
    private int sceneColorRenderbuffer;
    private int sceneDepthRenderbuffer;
    private int resolvedFramebuffer;
    private int resolvedColorTexture;
    private int width;
    private int height;
    private int sampleCount;

    /** Creates an unrealized target that allocates storage on its first frame. */
    public ToneMappingTarget() {
        // OpenGL storage intentionally follows the first known framebuffer dimensions.
    }

    /**
     * Resizes storage when required and binds the HDR scene framebuffer.
     *
     * @param width positive framebuffer width
     * @param height positive framebuffer height
     * @param sampleCount non-negative multisample count
     */
    public void begin(int width, int height, int sampleCount) {
        if (this.width != width || this.height != height || this.sampleCount != sampleCount) {
            realize(width, height, sampleCount);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, sceneFramebuffer);
    }

    /**
     * Resolves multisampling when needed and presents the ACES-mapped image.
     *
     * @param program tone-mapping presentation program
     * @param exposure positive linear exposure multiplier
     */
    public void present(ToneMappingProgram program, float exposure) {
        if (sampleCount > 1) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, sceneFramebuffer);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, resolvedFramebuffer);
            glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glUseProgram(program.id());
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, resolvedColorTexture);
        glUniform1i(program.sceneLocation(), 0);
        glUniform1f(program.exposureLocation(), exposure);
        glBindVertexArray(vertexArray);
        glDrawArrays(PrimitiveTopology.TRIANGLES.openGlMode(), 0, 3);
        glBindVertexArray(0);
    }

    /** Restores the default framebuffer when scene rendering aborts before presentation. */
    public void cancel() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Deletes all framebuffer, renderbuffer, texture, and vertex-array names. */
    @Override
    public void close() {
        releaseStorage();
        glDeleteVertexArrays(vertexArray);
    }

    /** Recreates complete HDR and optional multisample storage. */
    private void realize(int width, int height, int sampleCount) {
        releaseStorage();
        this.width = width;
        this.height = height;
        this.sampleCount = sampleCount;

        resolvedColorTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, resolvedColorTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        if (sampleCount > 1) {
            resolvedFramebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, resolvedFramebuffer);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, resolvedColorTexture, 0);
            requireComplete("resolved");

            sceneFramebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, sceneFramebuffer);
            sceneColorRenderbuffer = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, sceneColorRenderbuffer);
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, GL_RGBA16F, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, sceneColorRenderbuffer);
        } else {
            sceneFramebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, sceneFramebuffer);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, resolvedColorTexture, 0);
        }

        sceneDepthRenderbuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, sceneDepthRenderbuffer);
        if (sampleCount > 1) {
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, sampleCount, GL_DEPTH_COMPONENT24, width, height);
        } else {
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        }
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, sceneDepthRenderbuffer);
        requireComplete("scene");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Rejects unsupported or incomplete framebuffer configurations immediately. */
    private static void requireComplete(String label) {
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException(
                    "Cannot create " + label + " tone-mapping framebuffer: 0x" + Integer.toHexString(status));
        }
    }

    /** Releases resize-dependent storage while preserving the fullscreen vertex array. */
    private void releaseStorage() {
        if (sceneFramebuffer != 0) {
            glDeleteFramebuffers(sceneFramebuffer);
            sceneFramebuffer = 0;
        }
        if (resolvedFramebuffer != 0) {
            glDeleteFramebuffers(resolvedFramebuffer);
            resolvedFramebuffer = 0;
        }
        if (sceneColorRenderbuffer != 0) {
            glDeleteRenderbuffers(sceneColorRenderbuffer);
            sceneColorRenderbuffer = 0;
        }
        if (sceneDepthRenderbuffer != 0) {
            glDeleteRenderbuffers(sceneDepthRenderbuffer);
            sceneDepthRenderbuffer = 0;
        }
        if (resolvedColorTexture != 0) {
            glDeleteTextures(resolvedColorTexture);
            resolvedColorTexture = 0;
        }
    }
}
