/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.GL_R32F;
import static org.lwjgl.opengl.GL31.GL_MAX_TEXTURE_BUFFER_SIZE;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;
import static org.lwjgl.opengl.GL31.glTexBuffer;

import io.github.glynch.jscene3d.objects.InstancedMesh;
import io.github.glynch.jscene3d.objects.Mesh;
import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryStack;

/** Context-local texture buffer containing one mesh's ordinary or per-instance morph weights. */
public final class MorphWeightResource implements AutoCloseable {
    private final int buffer;
    private final int texture;
    private final int maximumTexels;
    private float[] uploaded;
    private float[] incoming;
    private long uploadedVersion = -1L;
    private boolean storageAllocated;

    /** Allocates unrealized buffer and texture names. */
    public MorphWeightResource() {
        buffer = glGenBuffers();
        texture = glGenTextures();
        maximumTexels = glGetInteger(GL_MAX_TEXTURE_BUFFER_SIZE);
        uploaded = new float[0];
        incoming = new float[0];
    }

    /**
     * Synchronizes changed weights and binds them to the requested texture unit.
     *
     * @param mesh source mesh
     * @param textureUnit texture unit receiving the buffer texture
     * @return upload activity produced by synchronization
     */
    public UploadResult synchronizeAndBind(Mesh mesh, int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        int rows = mesh instanceof InstancedMesh instancedMesh && instancedMesh.hasInstanceMorphTargetInfluences()
                ? instancedMesh.capacity()
                : 1;
        int length = Math.multiplyExact(rows, mesh.morphTargetCount());
        ensureCapacity(length);
        long version = sourceVersion(mesh);
        UploadResult result = UploadResult.NONE;
        if (version != uploadedVersion) {
            copyWeights(mesh);
            result = uploadChangedWeights();
            float[] previous = uploaded;
            uploaded = incoming;
            incoming = previous;
            uploadedVersion = version;
        }
        glBindTexture(GL_TEXTURE_BUFFER, texture);
        return result;
    }

    /** Recreates CPU and GPU storage when a mesh's morph layout changes. */
    private void ensureCapacity(int length) {
        if (length > maximumTexels) {
            throw new IllegalStateException("Morph weights require "
                    + length
                    + " texture-buffer texels, but this context supports "
                    + maximumTexels);
        }
        if (incoming.length == length) {
            return;
        }
        uploaded = new float[length];
        incoming = new float[length];
        storageAllocated = false;
        uploadedVersion = -1L;
    }

    /** Returns the version which captures all weight data consumed by the draw. */
    private static long sourceVersion(Mesh mesh) {
        if (mesh instanceof InstancedMesh instancedMesh) {
            return instancedMesh.instanceMorphTargetInfluenceVersion();
        }
        return mesh.morphTargetInfluenceVersion();
    }

    /** Copies ordinary or capacity-major instance weights into reusable staging. */
    private void copyWeights(Mesh mesh) {
        if (mesh instanceof InstancedMesh instancedMesh && instancedMesh.hasInstanceMorphTargetInfluences()) {
            instancedMesh.copyInstanceMorphTargetInfluencesTo(incoming);
        } else {
            mesh.copyMorphTargetInfluencesTo(incoming);
        }
    }

    /** Uploads initial storage or the smallest scalar span containing all changes. */
    private UploadResult uploadChangedWeights() {
        glBindBuffer(GL_TEXTURE_BUFFER, buffer);
        if (!storageAllocated) {
            glBufferData(GL_TEXTURE_BUFFER, incoming, GL_DYNAMIC_DRAW);
            glBindTexture(GL_TEXTURE_BUFFER, texture);
            glTexBuffer(GL_TEXTURE_BUFFER, GL_R32F, buffer);
            storageAllocated = true;
            return new UploadResult(1, incoming.length * (long) Float.BYTES);
        }
        int first = firstChangedIndex();
        if (first == incoming.length) {
            return UploadResult.NONE;
        }
        int last = lastChangedIndex(first);
        int length = last - first + 1;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer changed = stack.mallocFloat(length);
            changed.put(incoming, first, length).flip();
            glBufferSubData(GL_TEXTURE_BUFFER, (long) first * Float.BYTES, changed);
        }
        return new UploadResult(1, length * (long) Float.BYTES);
    }

    /** Finds the first changed scalar. */
    private int firstChangedIndex() {
        int index = 0;
        while (index < incoming.length && incoming[index] == uploaded[index]) {
            index++;
        }
        return index;
    }

    /** Finds the final changed scalar after a known first change. */
    private int lastChangedIndex(int first) {
        int index = incoming.length - 1;
        while (index > first && incoming[index] == uploaded[index]) {
            index--;
        }
        return index;
    }

    /** Deletes owned context-local names. */
    @Override
    public void close() {
        glDeleteTextures(texture);
        glDeleteBuffers(buffer);
    }

    /**
     * Upload activity produced by one synchronization.
     *
     * @param count number of OpenGL upload operations performed
     * @param byteCount number of bytes uploaded
     */
    public record UploadResult(int count, long byteCount) {
        private static final UploadResult NONE = new UploadResult(0, 0L);
    }
}
