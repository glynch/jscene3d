/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

/** Complete one-pixel white texture bound when a shader's optional sampler is disabled. */
public final class DefaultTexture implements AutoCloseable {
    private final int textureName;

    /** Allocates and defines the complete fallback image. */
    public DefaultTexture() {
        ByteBuffer whitePixel = BufferUtils.createByteBuffer(4);
        whitePixel
                .put((byte) 0xff)
                .put((byte) 0xff)
                .put((byte) 0xff)
                .put((byte) 0xff)
                .flip();
        int generatedTextureName = glGenTextures();
        try {
            glBindTexture(GL_TEXTURE_2D, generatedTextureName);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, whitePixel);
            textureName = generatedTextureName;
        } catch (RuntimeException exception) {
            glDeleteTextures(generatedTextureName);
            throw exception;
        }
    }

    /** Binds the complete fallback image to the current two-dimensional texture unit. */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureName);
    }

    /** Deletes the context-local fallback texture name. */
    @Override
    public void close() {
        glDeleteTextures(textureName);
    }
}
