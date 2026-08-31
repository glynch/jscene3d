/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.BufferUsage;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import org.jspecify.annotations.Nullable;

/** Context-local GPU realization of one buffer geometry. */
public final class GeometryResource implements AutoCloseable {
    private static final int POSITION_LOCATION = 0;
    private static final int NORMAL_LOCATION = 1;
    private static final int UV_LOCATION = 2;
    private static final int COLOR_LOCATION = 3;

    private final int vertexArray;

    private @Nullable AttributeResource positions;
    private @Nullable AttributeResource normals;
    private @Nullable AttributeResource colors;
    private @Nullable AttributeResource uvs;
    private @Nullable IndexResource indices;

    /** Allocates the vertex-array object that owns geometry bindings. */
    public GeometryResource() {
        vertexArray = glGenVertexArrays();
    }

    /**
     * Synchronizes changed CPU attributes and indices into context-local GPU buffers.
     *
     * @param geometry geometry to synchronize
     * @param requiresNormals whether the material requires normals
     * @param requiresVertexColors whether the material requires vertex colors
     * @param requiresTextureCoordinates whether the material requires texture coordinates
     * @param materialLabel material label used in diagnostics
     * @return uploads performed by this synchronization
     */
    public UploadResult synchronize(
            BufferGeometry geometry,
            boolean requiresNormals,
            boolean requiresVertexColors,
            boolean requiresTextureCoordinates,
            String materialLabel) {
        @Nullable BufferAttribute positionAttribute = geometry.attribute(BufferGeometry.POSITION);
        @Nullable BufferAttribute normalAttribute = geometry.attribute(BufferGeometry.NORMAL);
        @Nullable BufferAttribute colorAttribute = geometry.attribute(BufferGeometry.COLOR);
        @Nullable BufferAttribute uvAttribute = geometry.attribute(BufferGeometry.UV);
        if (requiresNormals && normalAttribute == null) {
            throw new IllegalStateException(materialLabel + " requires a normal attribute but geometry has none");
        }
        if (normalAttribute != null && normalAttribute.itemSize() != 3) {
            throw new IllegalStateException("normal attribute itemSize must be 3: " + normalAttribute.itemSize());
        }
        if (requiresVertexColors && colorAttribute == null) {
            throw new IllegalStateException(materialLabel + " requires a color attribute but geometry has none");
        }
        if (colorAttribute != null && colorAttribute.itemSize() != 3 && colorAttribute.itemSize() != 4) {
            throw new IllegalStateException("color attribute itemSize must be 3 or 4: " + colorAttribute.itemSize());
        }
        if (requiresTextureCoordinates && uvAttribute == null) {
            throw new IllegalStateException(materialLabel + " requires a uv attribute but geometry has none");
        }
        if (uvAttribute != null && uvAttribute.itemSize() != 2) {
            throw new IllegalStateException("uv attribute itemSize must be 2: " + uvAttribute.itemSize());
        }

        UploadCounter uploads = new UploadCounter();
        glBindVertexArray(vertexArray);
        positions = synchronizeAttribute(positions, positionAttribute, POSITION_LOCATION, 3, true, uploads);
        normals = synchronizeAttribute(normals, normalAttribute, NORMAL_LOCATION, 3, false, uploads);
        colors = synchronizeAttribute(
                colors,
                colorAttribute,
                COLOR_LOCATION,
                colorAttribute == null ? 4 : colorAttribute.itemSize(),
                false,
                uploads);
        uvs = synchronizeAttribute(uvs, uvAttribute, UV_LOCATION, 2, false, uploads);
        indices = synchronizeIndex(indices, geometry.index(), uploads);
        glBindVertexArray(0);
        return uploads.result();
    }

    /** Binds this geometry's vertex-array object for drawing. */
    public void bind() {
        glBindVertexArray(vertexArray);
    }

    @Override
    public void close() {
        closeAttribute(positions);
        closeAttribute(normals);
        closeAttribute(colors);
        closeAttribute(uvs);
        if (indices != null) {
            glDeleteBuffers(indices.buffer);
        }
        glDeleteVertexArrays(vertexArray);
    }

    /** Reuses or replaces one attribute buffer and uploads it only when its version changed. */
    private static @Nullable AttributeResource synchronizeAttribute(
            @Nullable AttributeResource existing,
            @Nullable BufferAttribute attribute,
            int location,
            int itemSize,
            boolean required,
            UploadCounter uploads) {
        if (attribute == null) {
            if (required) {
                throw new IllegalStateException("Drawable geometry has no position attribute");
            }
            closeAttribute(existing);
            glDisableVertexAttribArray(location);
            return null;
        }

        AttributeResource resource = existing;
        if (resource == null || resource.attribute != attribute) {
            closeAttribute(resource);
            resource = new AttributeResource(attribute, glGenBuffers());
        }
        glBindBuffer(GL_ARRAY_BUFFER, resource.buffer);
        if (resource.uploadedVersion != attribute.version()) {
            attribute.copyTo(resource.staging);
            glBufferData(GL_ARRAY_BUFFER, resource.staging, toOpenGlUsage(attribute.usage()));
            resource.uploadedVersion = attribute.version();
            uploads.recordUpload((long) resource.staging.length * Float.BYTES);
        }
        glEnableVertexAttribArray(location);
        glVertexAttribPointer(location, itemSize, GL_FLOAT, false, 0, 0L);
        return resource;
    }

    /** Reuses or replaces the index buffer and uploads it only when its version changed. */
    private static @Nullable IndexResource synchronizeIndex(
            @Nullable IndexResource existing, @Nullable IndexBuffer index, UploadCounter uploads) {
        if (index == null) {
            if (existing != null) {
                glDeleteBuffers(existing.buffer);
            }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            return null;
        }

        IndexResource resource = existing;
        if (resource == null || resource.index != index) {
            if (resource != null) {
                glDeleteBuffers(resource.buffer);
            }
            resource = new IndexResource(index, glGenBuffers());
        }
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, resource.buffer);
        if (resource.uploadedVersion != index.version()) {
            index.copyTo(resource.staging);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, resource.staging, toOpenGlUsage(index.usage()));
            resource.uploadedVersion = index.version();
            uploads.recordUpload((long) resource.staging.length * Integer.BYTES);
        }
        return resource;
    }

    /** Converts renderer-independent buffer usage to an OpenGL usage hint. */
    private static int toOpenGlUsage(BufferUsage usage) {
        return switch (usage) {
            case STATIC -> GL_STATIC_DRAW;
            case DYNAMIC -> GL_DYNAMIC_DRAW;
            case STREAM -> GL_STREAM_DRAW;
        };
    }

    /** Deletes an attribute buffer when one is present. */
    private static void closeAttribute(@Nullable AttributeResource resource) {
        if (resource != null) {
            glDeleteBuffers(resource.buffer);
        }
    }

    /** Context-local attribute buffer with reusable CPU upload staging. */
    private static final class AttributeResource {
        private final BufferAttribute attribute;
        private final int buffer;
        private final float[] staging;

        private long uploadedVersion = -1L;

        /** Associates one attribute description with a newly allocated buffer name. */
        private AttributeResource(BufferAttribute attribute, int buffer) {
            this.attribute = attribute;
            this.buffer = buffer;
            staging = new float[Math.multiplyExact(attribute.count(), attribute.itemSize())];
        }
    }

    /** Context-local index buffer with reusable CPU upload staging. */
    private static final class IndexResource {
        private final IndexBuffer index;
        private final int buffer;
        private final int[] staging;

        private long uploadedVersion = -1L;

        /** Associates one index description with a newly allocated buffer name. */
        private IndexResource(IndexBuffer index, int buffer) {
            this.index = index;
            this.buffer = buffer;
            staging = new int[index.count()];
        }
    }

    /**
     * Upload activity produced by one synchronization.
     *
     * @param count upload count
     * @param byteCount combined uploaded byte count
     */
    public record UploadResult(int count, long byteCount) {}

    /** Mutable upload aggregation confined to one synchronization. */
    private static final class UploadCounter {
        private int count;
        private long byteCount;

        /** Records one upload. */
        private void recordUpload(long uploadedBytes) {
            count++;
            byteCount += uploadedBytes;
        }

        /** Returns immutable accumulated values. */
        private UploadResult result() {
            return new UploadResult(count, byteCount);
        }
    }
}
