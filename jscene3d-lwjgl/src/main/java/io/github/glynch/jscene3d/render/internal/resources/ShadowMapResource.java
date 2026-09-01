/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glReadBuffer;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30.GL_COMPARE_REF_TO_TEXTURE;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

/** Context-local depth texture and framebuffer used by one shadow-casting light. */
public final class ShadowMapResource implements AutoCloseable {
    private final boolean cube;
    private final int framebuffer;
    private final int texture;

    private int width;
    private int height;

    /**
     * Allocates names for an initially unrealized two-dimensional or cube shadow map.
     *
     * @param cube whether to allocate cube faces instead of one two-dimensional image
     */
    public ShadowMapResource(boolean cube) {
        this.cube = cube;
        framebuffer = glGenFramebuffers();
        texture = glGenTextures();
    }

    /**
     * Ensures depth storage has the requested dimensions.
     *
     * @param width positive map width
     * @param height positive map height
     */
    public void realize(int width, int height) {
        if (this.width == width && this.height == height) {
            return;
        }
        this.width = width;
        this.height = height;
        int target = cube ? GL_TEXTURE_CUBE_MAP : GL_TEXTURE_2D;
        glBindTexture(target, texture);
        if (cube) {
            for (int face = 0; face < 6; face++) {
                allocate(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, width, height);
            }
        } else {
            allocate(GL_TEXTURE_2D, width, height);
        }
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        if (cube) {
            glTexParameteri(target, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        }
        glTexParameteri(target, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        glTexParameteri(target, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        attachFace(0);
    }

    /**
     * Binds this map's framebuffer and selects one cube face when applicable.
     *
     * @param face zero-based cube face, ignored for a two-dimensional map
     */
    public void bindForWriting(int face) {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        if (cube) {
            attachFace(face);
        }
    }

    /** Binds the depth texture to the active texture unit. */
    public void bindTexture() {
        glBindTexture(cube ? GL_TEXTURE_CUBE_MAP : GL_TEXTURE_2D, texture);
    }

    /**
     * Returns whether this resource represents a cube map.
     *
     * @return {@code true} for a six-face cube map
     */
    public boolean isCube() {
        return cube;
    }

    /** Releases the framebuffer and depth texture. */
    @Override
    public void close() {
        glDeleteFramebuffers(framebuffer);
        glDeleteTextures(texture);
    }

    /** Allocates one depth image. */
    private static void allocate(int target, int width, int height) {
        glTexImage2D(target, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
    }

    /** Attaches one two-dimensional image or cube face and validates completeness. */
    private void attachFace(int face) {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        int target = cube ? GL_TEXTURE_CUBE_MAP_POSITIVE_X + face : GL_TEXTURE_2D;
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, target, texture, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Cannot create shadow-map framebuffer: 0x" + Integer.toHexString(status));
        }
    }
}
