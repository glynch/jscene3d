/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

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

import io.github.glynch.jscene3d.core.BufferAttribute;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.BufferUsage;
import io.github.glynch.jscene3d.core.IndexBuffer;
import org.jspecify.annotations.Nullable;

/** Context-local GPU realization of one buffer geometry. */
final class GeometryResource implements AutoCloseable {
    private static final int POSITION_LOCATION = 0;
    private static final int COLOR_LOCATION = 1;

    private final int vertexArray;

    private @Nullable AttributeResource positions;
    private @Nullable AttributeResource colors;
    private @Nullable IndexResource indices;

    /** Allocates the vertex-array object that owns geometry bindings. */
    GeometryResource() {
        vertexArray = glGenVertexArrays();
    }

    /** Synchronizes changed CPU attributes and indices into context-local GPU buffers. */
    void synchronize(BufferGeometry geometry, boolean requiresVertexColors, RenderStatistics statistics) {
        @Nullable BufferAttribute positionAttribute = geometry.attribute(BufferGeometry.POSITION);
        @Nullable BufferAttribute colorAttribute = geometry.attribute(BufferGeometry.COLOR);
        if (requiresVertexColors && colorAttribute == null) {
            throw new IllegalStateException("BasicMaterial uses vertex colors but geometry has no color attribute");
        }
        if (colorAttribute != null && colorAttribute.itemSize() != 3 && colorAttribute.itemSize() != 4) {
            throw new IllegalStateException("color attribute itemSize must be 3 or 4: " + colorAttribute.itemSize());
        }

        glBindVertexArray(vertexArray);
        positions = synchronizeAttribute(positions, positionAttribute, POSITION_LOCATION, 3, true, statistics);
        colors = synchronizeAttribute(
                colors,
                colorAttribute,
                COLOR_LOCATION,
                colorAttribute == null ? 4 : colorAttribute.itemSize(),
                false,
                statistics);
        indices = synchronizeIndex(indices, geometry.index(), statistics);
        glBindVertexArray(0);
    }

    /** Binds this geometry's vertex-array object for drawing. */
    void bind() {
        glBindVertexArray(vertexArray);
    }

    @Override
    public void close() {
        closeAttribute(positions);
        closeAttribute(colors);
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
            RenderStatistics statistics) {
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
            statistics.recordUpload((long) resource.staging.length * Float.BYTES);
        }
        glEnableVertexAttribArray(location);
        glVertexAttribPointer(location, itemSize, GL_FLOAT, false, 0, 0L);
        return resource;
    }

    /** Reuses or replaces the index buffer and uploads it only when its version changed. */
    private static @Nullable IndexResource synchronizeIndex(
            @Nullable IndexResource existing, @Nullable IndexBuffer index, RenderStatistics statistics) {
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
            statistics.recordUpload((long) resource.staging.length * Integer.BYTES);
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
}
