/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT;
import static org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import io.github.glynch.jscene3d.textures.MipmapMode;
import io.github.glynch.jscene3d.textures.Texture;
import io.github.glynch.jscene3d.textures.TextureColorSpace;
import io.github.glynch.jscene3d.textures.TextureFilter;
import io.github.glynch.jscene3d.textures.TextureWrap;
import java.nio.ByteBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/** Context-local GPU realization of one renderer-independent texture. */
public final class TextureResource implements AutoCloseable {
    private final int textureName;

    private @Nullable ByteBuffer staging;
    private long uploadedImageVersion = -1L;
    private long appliedSamplerVersion = -1L;
    private long mipmapImageVersion = -1L;

    /** Allocates one context-local OpenGL texture name. */
    public TextureResource() {
        textureName = glGenTextures();
    }

    /**
     * Synchronizes changed image and sampler state, then leaves this texture bound.
     *
     * @param texture texture description to synchronize
     * @return uploaded image bytes, or zero when no image upload occurred
     */
    public long synchronize(Texture texture) {
        glBindTexture(GL_TEXTURE_2D, textureName);
        long imageVersion = texture.imageVersion();
        long uploadedBytes = 0L;
        if (uploadedImageVersion != imageVersion) {
            uploadedBytes = uploadImage(texture);
            uploadedImageVersion = imageVersion;
            mipmapImageVersion = -1L;
        }
        if (appliedSamplerVersion != texture.samplerVersion()) {
            applySampler(texture);
            appliedSamplerVersion = texture.samplerVersion();
        }
        if (texture.mipmapMode() == MipmapMode.GENERATE && mipmapImageVersion != imageVersion) {
            glGenerateMipmap(GL_TEXTURE_2D);
            mipmapImageVersion = imageVersion;
        }
        return uploadedBytes;
    }

    /** Releases this realization's OpenGL name and native upload staging. */
    @Override
    public void close() {
        if (staging != null) {
            MemoryUtil.memFree(staging);
            staging = null;
        }
        glDeleteTextures(textureName);
    }

    /** Copies retained CPU pixels and defines the OpenGL base image. */
    private long uploadImage(Texture texture) {
        int byteCount = texture.pixelByteCount();
        if (staging == null || staging.capacity() != byteCount) {
            ByteBuffer replacement = MemoryUtil.memAlloc(byteCount);
            if (staging != null) {
                MemoryUtil.memFree(staging);
            }
            staging = replacement;
        }
        ByteBuffer pixels = staging;
        pixels.clear();
        texture.copyPixelsTo(pixels);
        pixels.flip();
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        int internalFormat = texture.colorSpace() == TextureColorSpace.SRGB ? GL_SRGB8_ALPHA8 : GL_RGBA8;
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                internalFormat,
                texture.width(),
                texture.height(),
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                pixels);
        return byteCount;
    }

    /** Applies minification, magnification, and coordinate-wrap parameters. */
    private static void applySampler(Texture texture) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, toOpenGlFilter(texture.minificationFilter()));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, toOpenGlFilter(texture.magnificationFilter()));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, toOpenGlWrap(texture.horizontalWrap()));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, toOpenGlWrap(texture.verticalWrap()));
    }

    /** Converts a renderer-independent filter into an OpenGL texture parameter. */
    private static int toOpenGlFilter(TextureFilter filter) {
        return switch (filter) {
            case NEAREST -> GL_NEAREST;
            case LINEAR -> GL_LINEAR;
            case NEAREST_MIPMAP_NEAREST -> GL_NEAREST_MIPMAP_NEAREST;
            case LINEAR_MIPMAP_NEAREST -> GL_LINEAR_MIPMAP_NEAREST;
            case NEAREST_MIPMAP_LINEAR -> GL_NEAREST_MIPMAP_LINEAR;
            case LINEAR_MIPMAP_LINEAR -> GL_LINEAR_MIPMAP_LINEAR;
        };
    }

    /** Converts a renderer-independent wrap mode into an OpenGL texture parameter. */
    private static int toOpenGlWrap(TextureWrap wrap) {
        return switch (wrap) {
            case CLAMP_TO_EDGE -> GL_CLAMP_TO_EDGE;
            case REPEAT -> GL_REPEAT;
            case MIRRORED_REPEAT -> GL_MIRRORED_REPEAT;
        };
    }
}
