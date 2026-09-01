/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

import io.github.glynch.jscene3d.objects.InstancedMesh;
import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryStack;

/** Context-local GPU transform and color buffers for one instanced mesh. */
public final class InstanceResource implements AutoCloseable {
    /** First of four consecutive matrix-column attribute locations. */
    public static final int MATRIX_LOCATION = 7;

    /** Optional linear RGB instance-color attribute location. */
    public static final int COLOR_LOCATION = 11;

    private static final int MATRIX_STRIDE_BYTES = InstancedMesh.MATRIX_COMPONENTS * Float.BYTES;
    private static final int COLOR_STRIDE_BYTES = InstancedMesh.COLOR_COMPONENTS * Float.BYTES;

    private final int matrixBuffer;
    private float[] matrixStaging;
    private float[] matrixIncoming;
    private float[] colorStaging;
    private float[] colorIncoming;

    private int colorBuffer;
    private long uploadedMatrixVersion = -1L;
    private long uploadedColorVersion = -1L;
    private boolean matrixStorageAllocated;
    private boolean colorStorageAllocated;

    /**
     * Allocates retained CPU staging and one matrix-buffer name for a fixed-capacity batch.
     *
     * @param capacity positive batch capacity
     */
    public InstanceResource(int capacity) {
        matrixBuffer = glGenBuffers();
        matrixStaging = new float[Math.multiplyExact(capacity, InstancedMesh.MATRIX_COMPONENTS)];
        matrixIncoming = new float[matrixStaging.length];
        colorStaging = new float[Math.multiplyExact(capacity, InstancedMesh.COLOR_COMPONENTS)];
        colorIncoming = new float[colorStaging.length];
    }

    /**
     * Synchronizes changed data and binds instanced attributes onto the currently bound geometry
     * vertex array.
     *
     * @param mesh source batch
     * @return upload activity produced by this synchronization
     */
    public UploadResult synchronizeAndBind(InstancedMesh mesh) {
        UploadCounter uploads = new UploadCounter();
        synchronizeMatrices(mesh, uploads);
        bindMatrices();
        synchronizeColors(mesh, uploads);
        return uploads.result();
    }

    /** Uploads an initial matrix allocation or only the scalar span changed since the last draw. */
    private void synchronizeMatrices(InstancedMesh mesh, UploadCounter uploads) {
        if (uploadedMatrixVersion == mesh.matrixVersion()) {
            return;
        }
        mesh.copyMatricesTo(matrixIncoming);
        glBindBuffer(GL_ARRAY_BUFFER, matrixBuffer);
        if (!matrixStorageAllocated) {
            glBufferData(GL_ARRAY_BUFFER, matrixIncoming, GL_DYNAMIC_DRAW);
            matrixStorageAllocated = true;
            uploads.noteUpload(matrixIncoming.length * (long) Float.BYTES);
        } else {
            uploadChangedRange(matrixStaging, matrixIncoming, uploads);
        }
        float[] previous = matrixStaging;
        matrixStaging = matrixIncoming;
        matrixIncoming = previous;
        uploadedMatrixVersion = mesh.matrixVersion();
    }

    /** Configures four per-instance matrix columns on the active geometry vertex array. */
    private void bindMatrices() {
        glBindBuffer(GL_ARRAY_BUFFER, matrixBuffer);
        for (int column = 0; column < 4; column++) {
            int location = MATRIX_LOCATION + column;
            glEnableVertexAttribArray(location);
            glVertexAttribPointer(location, 4, GL_FLOAT, false, MATRIX_STRIDE_BYTES, (long) column * 4 * Float.BYTES);
            glVertexAttribDivisor(location, 1);
        }
    }

    /** Synchronizes and binds optional colors, or disables their attribute when absent. */
    private void synchronizeColors(InstancedMesh mesh, UploadCounter uploads) {
        if (!mesh.hasInstanceColors()) {
            glDisableVertexAttribArray(COLOR_LOCATION);
            glVertexAttribDivisor(COLOR_LOCATION, 0);
            uploadedColorVersion = mesh.colorVersion();
            return;
        }
        if (colorBuffer == 0) {
            colorBuffer = glGenBuffers();
        }
        if (uploadedColorVersion != mesh.colorVersion()) {
            mesh.copyColorsTo(colorIncoming);
            glBindBuffer(GL_ARRAY_BUFFER, colorBuffer);
            if (!colorStorageAllocated) {
                glBufferData(GL_ARRAY_BUFFER, colorIncoming, GL_DYNAMIC_DRAW);
                colorStorageAllocated = true;
                uploads.noteUpload(colorIncoming.length * (long) Float.BYTES);
            } else {
                uploadChangedRange(colorStaging, colorIncoming, uploads);
            }
            float[] previous = colorStaging;
            colorStaging = colorIncoming;
            colorIncoming = previous;
            uploadedColorVersion = mesh.colorVersion();
        }
        glBindBuffer(GL_ARRAY_BUFFER, colorBuffer);
        glEnableVertexAttribArray(COLOR_LOCATION);
        glVertexAttribPointer(COLOR_LOCATION, 3, GL_FLOAT, false, COLOR_STRIDE_BYTES, 0L);
        glVertexAttribDivisor(COLOR_LOCATION, 1);
    }

    /** Finds and uploads the smallest contiguous scalar range containing every changed value. */
    private static void uploadChangedRange(float[] previous, float[] current, UploadCounter uploads) {
        int first = 0;
        while (first < current.length && previous[first] == current[first]) {
            first++;
        }
        if (first == current.length) {
            return;
        }
        int last = current.length - 1;
        while (last > first && previous[last] == current[last]) {
            last--;
        }
        int length = last - first + 1;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer changed = stack.mallocFloat(length);
            changed.put(current, first, length).flip();
            glBufferSubData(GL_ARRAY_BUFFER, (long) first * Float.BYTES, changed);
        }
        uploads.noteUpload(length * (long) Float.BYTES);
    }

    /** Deletes owned context-local buffer names. */
    @Override
    public void close() {
        glDeleteBuffers(matrixBuffer);
        if (colorBuffer != 0) {
            glDeleteBuffers(colorBuffer);
            colorBuffer = 0;
        }
    }

    /**
     * Upload activity produced by one synchronization.
     *
     * @param count upload count
     * @param byteCount combined byte count
     */
    public record UploadResult(int count, long byteCount) {}

    /** Mutable upload counter confined to one call. */
    private static final class UploadCounter {
        private int count;
        private long byteCount;

        private void noteUpload(long bytes) {
            count++;
            byteCount += bytes;
        }

        private UploadResult result() {
            return new UploadResult(count, byteCount);
        }
    }
}
