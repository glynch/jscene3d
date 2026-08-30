/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;

/** Context-local drawing resources for renderer-owned two-dimensional overlays. */
final class OverlayRenderer implements AutoCloseable {
    private static final int COMPONENTS_PER_VERTEX = 8;
    private static final int STRIDE_BYTES = COMPONENTS_PER_VERTEX * Float.BYTES;
    private static final int INITIAL_COMPONENT_CAPACITY = COMPONENTS_PER_VERTEX * 1024;

    private final OverlayProgram program;
    private final int vertexArray;
    private final int vertexBuffer;
    private final IdentityHashMap<OverlayImage, Integer> textures = new IdentityHashMap<>();

    private FloatBuffer staging = BufferUtils.createFloatBuffer(INITIAL_COMPONENT_CAPACITY);

    /** Creates the program, vertex array, and dynamic vertex buffer atomically. */
    private OverlayRenderer(OverlayProgram program, int vertexArray, int vertexBuffer) {
        this.program = program;
        this.vertexArray = vertexArray;
        this.vertexBuffer = vertexBuffer;
    }

    /** Creates context-local overlay resources or releases partial construction on failure. */
    static OverlayRenderer create() {
        OverlayProgram program = OverlayProgram.create();
        int vertexArray = 0;
        int vertexBuffer = 0;
        try {
            vertexArray = glGenVertexArrays();
            vertexBuffer = glGenBuffers();
            glBindVertexArray(vertexArray);
            glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, STRIDE_BYTES, 0L);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE_BYTES, 2L * Float.BYTES);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 4, GL_FLOAT, false, STRIDE_BYTES, 4L * Float.BYTES);
            glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            return new OverlayRenderer(program, vertexArray, vertexBuffer);
        } catch (RuntimeException exception) {
            if (vertexBuffer != 0) {
                glDeleteBuffers(vertexBuffer);
            }
            if (vertexArray != 0) {
                glDeleteVertexArrays(vertexArray);
            }
            program.close();
            throw exception;
        }
    }

    /** Uploads and draws accumulated overlay triangles with explicit renderer-owned state. */
    void render(
            OverlayCanvas canvas, int logicalWidth, int logicalHeight, int framebufferWidth, int framebufferHeight) {
        int commandCount = canvas.commandCount();
        int vertexCount = canvas.vertexCount();
        if (vertexCount == 0 || framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }
        int componentCount = vertexCount * COMPONENTS_PER_VERTEX;
        ensureStagingCapacity(componentCount);
        staging.clear();
        staging.put(canvas.vertices(), 0, componentCount).flip();

        prepareState(logicalWidth, logicalHeight, framebufferWidth, framebufferHeight);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, staging, GL_DYNAMIC_DRAW);
        for (int command = 0; command < commandCount; command++) {
            OverlayImage image = canvas.commandImage(command);
            bindImage(image);
            glDrawArrays(GL_TRIANGLES, canvas.commandStart(command), canvas.commandVertexCount(command));
        }
        resetBindings();
    }

    /** Applies all state required by an overlay draw. */
    private void prepareState(int logicalWidth, int logicalHeight, int framebufferWidth, int framebufferHeight) {
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        glUseProgram(program.id());
        glUniform2f(program.logicalSizeLocation(), logicalWidth, logicalHeight);
        glUniform1i(program.alphaMaskLocation(), 0);
        glActiveTexture(GL_TEXTURE0);
        glBindVertexArray(vertexArray);
    }

    /** Binds one lazily uploaded alpha mask, or selects solid-color drawing. */
    private void bindImage(@Nullable OverlayImage image) {
        if (image == null) {
            glUniform1i(program.usesAlphaMaskLocation(), 0);
            glBindTexture(GL_TEXTURE_2D, 0);
            return;
        }
        glUniform1i(program.usesAlphaMaskLocation(), 1);
        glBindTexture(GL_TEXTURE_2D, textures.computeIfAbsent(image, OverlayRenderer::createTexture));
    }

    /** Uploads one immutable alpha mask as a context-local texture. */
    private static int createTexture(OverlayImage image) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(image.pixels().length);
        pixels.put(image.pixels()).flip();
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, image.width(), image.height(), 0, GL_RED, GL_UNSIGNED_BYTE, pixels);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        return texture;
    }

    /** Removes transient bindings while restoring writable depth state. */
    private static void resetBindings() {
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glUseProgram(0);
        glDepthMask(true);
    }

    /** Grows direct staging storage geometrically. */
    private void ensureStagingCapacity(int componentCount) {
        if (componentCount > staging.capacity()) {
            staging = BufferUtils.createFloatBuffer(Math.max(componentCount, staging.capacity() * 2));
        }
    }

    /** Releases all context-local overlay resources. */
    @Override
    public void close() {
        for (int texture : textures.values()) {
            glDeleteTextures(texture);
        }
        textures.clear();
        glDeleteBuffers(vertexBuffer);
        glDeleteVertexArrays(vertexArray);
        program.close();
    }
}
