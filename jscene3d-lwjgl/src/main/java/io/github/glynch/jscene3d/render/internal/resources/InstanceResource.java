/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttrib3f;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferUsage;
import io.github.glynch.jscene3d.objects.InstancedMesh;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.lwjgl.system.MemoryStack;

/** Context-local GPU transform and color buffers for one instanced mesh. */
public final class InstanceResource implements AutoCloseable {
    /** First of four consecutive matrix-column attribute locations. */
    public static final int MATRIX_LOCATION = 7;

    /** Optional linear RGB instance-color attribute location. */
    public static final int COLOR_LOCATION = 11;

    /** First application-defined per-instance attribute location. */
    public static final int CUSTOM_LOCATION = 12;

    private static final int MATRIX_STRIDE_BYTES = InstancedMesh.MATRIX_COMPONENTS * Float.BYTES;
    private static final int COLOR_STRIDE_BYTES = InstancedMesh.COLOR_COMPONENTS * Float.BYTES;

    private final int matrixBuffer;
    private final Map<String, CustomAttributeResource> customAttributes;
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
        customAttributes = new LinkedHashMap<>();
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
        return synchronizeAndBind(mesh, Map.of());
    }

    /**
     * Synchronizes changed built-in and application-defined instance data.
     *
     * @param mesh source batch
     * @param requiredAttributes custom input names and required item sizes in binding order
     * @return upload activity produced by this synchronization
     */
    public UploadResult synchronizeAndBind(InstancedMesh mesh, Map<String, Integer> requiredAttributes) {
        UploadCounter uploads = new UploadCounter();
        synchronizeMatrices(mesh, uploads);
        bindMatrices();
        synchronizeColors(mesh, uploads);
        synchronizeCustomAttributes(mesh, requiredAttributes, uploads);
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
            glVertexAttrib3f(COLOR_LOCATION, 1.0f, 1.0f, 1.0f);
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

    /** Reconciles, uploads, and binds the custom inputs selected by the current material. */
    private void synchronizeCustomAttributes(
            InstancedMesh mesh, Map<String, Integer> requiredAttributes, UploadCounter uploads) {
        disableCustomAttributeLocations();
        removeInactiveCustomAttributes(requiredAttributes);
        int location = CUSTOM_LOCATION;
        for (Map.Entry<String, Integer> requirement : requiredAttributes.entrySet()) {
            String name = requirement.getKey();
            BufferAttribute attribute = mesh.instanceAttribute(name);
            if (attribute == null) {
                throw new IllegalStateException("InstancedMesh has no required instance attribute: " + name);
            }
            if (attribute.itemSize() != requirement.getValue()) {
                throw new IllegalStateException("InstancedMesh instance attribute "
                        + name
                        + " has itemSize "
                        + attribute.itemSize()
                        + ", expected "
                        + requirement.getValue());
            }
            CustomAttributeResource resource = customAttributes.get(name);
            if (resource == null || resource.source() != attribute) {
                if (resource != null) {
                    resource.close();
                }
                resource = new CustomAttributeResource(attribute);
                customAttributes.put(name, resource);
            }
            resource.synchronizeAndBind(location++, uploads);
        }
    }

    /** Removes GPU buffers and vertex-array bindings no longer selected by the current material. */
    private void removeInactiveCustomAttributes(Map<String, Integer> requiredAttributes) {
        Iterator<Map.Entry<String, CustomAttributeResource>> iterator =
                customAttributes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CustomAttributeResource> entry = iterator.next();
            if (!requiredAttributes.containsKey(entry.getKey())) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    /** Clears the portable four-location custom range before rebinding current declarations. */
    private static void disableCustomAttributeLocations() {
        for (int location = CUSTOM_LOCATION; location < CUSTOM_LOCATION + 4; location++) {
            glDisableVertexAttribArray(location);
            glVertexAttribDivisor(location, 0);
        }
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
        customAttributes.values().forEach(CustomAttributeResource::close);
        customAttributes.clear();
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

    /** Context-local buffer and retained comparison data for one custom instance input. */
    private static final class CustomAttributeResource implements AutoCloseable {
        private final BufferAttribute source;
        private final int buffer;
        private float[] staging;
        private float[] incoming;

        private long uploadedVersion = -1L;
        private boolean storageAllocated;

        private CustomAttributeResource(BufferAttribute source) {
            this.source = Objects.requireNonNull(source, "source");
            buffer = glGenBuffers();
            staging = new float[Math.multiplyExact(source.count(), source.itemSize())];
            incoming = new float[staging.length];
        }

        private BufferAttribute source() {
            return source;
        }

        private void synchronizeAndBind(int location, UploadCounter uploads) {
            glBindBuffer(GL_ARRAY_BUFFER, buffer);
            if (uploadedVersion != source.version()) {
                source.copyTo(incoming);
                if (!storageAllocated) {
                    glBufferData(GL_ARRAY_BUFFER, incoming, toOpenGlUsage(source.usage()));
                    storageAllocated = true;
                    uploads.noteUpload(incoming.length * (long) Float.BYTES);
                } else {
                    uploadChangedRange(staging, incoming, uploads);
                }
                float[] previous = staging;
                staging = incoming;
                incoming = previous;
                uploadedVersion = source.version();
            }
            glEnableVertexAttribArray(location);
            glVertexAttribPointer(location, source.itemSize(), GL_FLOAT, false, 0, 0L);
            glVertexAttribDivisor(location, 1);
        }

        @Override
        public void close() {
            glDeleteBuffers(buffer);
        }

        /** Maps the public mutation-frequency hint to its OpenGL allocation hint. */
        private static int toOpenGlUsage(BufferUsage usage) {
            return switch (usage) {
                case STATIC -> GL_STATIC_DRAW;
                case DYNAMIC -> GL_DYNAMIC_DRAW;
                case STREAM -> GL_STREAM_DRAW;
            };
        }
    }
}
