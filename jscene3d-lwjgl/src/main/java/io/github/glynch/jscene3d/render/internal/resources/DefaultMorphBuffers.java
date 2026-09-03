/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.GL_R32F;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;
import static org.lwjgl.opengl.GL31.glTexBuffer;

/** Complete zero-valued texture buffers bound while morph deformation is disabled. */
final class DefaultMorphBuffers implements AutoCloseable {
    private final TextureBuffer targets;
    private final TextureBuffer weights;

    /** Creates complete target-delta and weight buffers for the fixed morph sampler units. */
    DefaultMorphBuffers() {
        targets = new TextureBuffer(GL_RGBA32F, new float[4]);
        try {
            weights = new TextureBuffer(GL_R32F, new float[1]);
        } catch (RuntimeException exception) {
            targets.close();
            throw exception;
        }
    }

    /** Binds both complete fallbacks to the fixed units consumed by built-in programs. */
    void bind() {
        targets.bind(MorphResources.TARGET_TEXTURE_UNIT);
        weights.bind(MorphResources.WEIGHT_TEXTURE_UNIT);
    }

    /** Releases both context-local texture-buffer objects. */
    @Override
    public void close() {
        targets.close();
        weights.close();
    }

    /** One immutable zero-filled buffer paired with its texture-buffer view. */
    private static final class TextureBuffer implements AutoCloseable {
        private final int buffer;
        private final int texture;

        /** Allocates storage and attaches a typed texture view. */
        private TextureBuffer(int format, float[] values) {
            int generatedBuffer = glGenBuffers();
            int generatedTexture = glGenTextures();
            try {
                glBindBuffer(GL_TEXTURE_BUFFER, generatedBuffer);
                glBufferData(GL_TEXTURE_BUFFER, values, GL_STATIC_DRAW);
                glBindTexture(GL_TEXTURE_BUFFER, generatedTexture);
                glTexBuffer(GL_TEXTURE_BUFFER, format, generatedBuffer);
                buffer = generatedBuffer;
                texture = generatedTexture;
            } catch (RuntimeException exception) {
                glDeleteTextures(generatedTexture);
                glDeleteBuffers(generatedBuffer);
                throw exception;
            }
        }

        /** Binds this complete view to one combined texture unit. */
        private void bind(int textureUnit) {
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            glBindTexture(GL_TEXTURE_BUFFER, texture);
        }

        /** Releases the view before its backing buffer. */
        @Override
        public void close() {
            glDeleteTextures(texture);
            glDeleteBuffers(buffer);
        }
    }
}
