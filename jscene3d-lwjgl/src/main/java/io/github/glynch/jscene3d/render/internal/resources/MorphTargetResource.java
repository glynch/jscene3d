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
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL31.GL_MAX_TEXTURE_BUFFER_SIZE;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;
import static org.lwjgl.opengl.GL31.glTexBuffer;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.MorphTarget;
import java.util.ArrayList;
import java.util.List;

/** Context-local texture buffer containing one geometry's morph-position and normal deltas. */
public final class MorphTargetResource implements AutoCloseable {
    private final int buffer;
    private final int texture;
    private final List<AttributeVersion> versions;
    private final int maximumTexels;
    private long uploadedGeometryVersion = -1L;

    /** Allocates unrealized buffer and texture names. */
    public MorphTargetResource() {
        buffer = glGenBuffers();
        texture = glGenTextures();
        versions = new ArrayList<>();
        maximumTexels = glGetInteger(GL_MAX_TEXTURE_BUFFER_SIZE);
    }

    /**
     * Synchronizes changed target deltas and binds them to the requested texture unit.
     *
     * @param geometry source geometry
     * @param textureUnit texture unit receiving the buffer texture
     * @return upload activity produced by synchronization
     */
    public UploadResult synchronizeAndBind(BufferGeometry geometry, int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        boolean changed = uploadedGeometryVersion != geometry.version() || targetVersionsChanged(geometry);
        long uploadedBytes = 0L;
        if (changed) {
            float[] packed = pack(geometry);
            requireSupportedTexelCount(packed.length / 4);
            glBindBuffer(GL_TEXTURE_BUFFER, buffer);
            glBufferData(GL_TEXTURE_BUFFER, packed, GL_DYNAMIC_DRAW);
            glBindTexture(GL_TEXTURE_BUFFER, texture);
            glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32F, buffer);
            rememberVersions(geometry);
            uploadedGeometryVersion = geometry.version();
            uploadedBytes = packed.length * (long) Float.BYTES;
        }
        glBindTexture(GL_TEXTURE_BUFFER, texture);
        return new UploadResult(changed ? 1 : 0, uploadedBytes);
    }

    /** Rejects target layouts that exceed the context's texture-buffer capacity. */
    private void requireSupportedTexelCount(int texelCount) {
        if (texelCount > maximumTexels) {
            throw new IllegalStateException("Morph target data requires "
                    + texelCount
                    + " texture-buffer texels, but this context supports "
                    + maximumTexels);
        }
    }

    /** Returns whether any retained target attribute changed since the previous upload. */
    private boolean targetVersionsChanged(BufferGeometry geometry) {
        List<MorphTarget> targets = geometry.morphTargets();
        if (versions.size() != targets.size() * 2) {
            return true;
        }
        int versionIndex = 0;
        for (MorphTarget target : targets) {
            if (!versions.get(versionIndex++).matches(target.positions())) {
                return true;
            }
            BufferAttribute normals = target.normals().orElse(null);
            if (!versions.get(versionIndex++).matches(normals)) {
                return true;
            }
        }
        return false;
    }

    /** Packs alternating position and normal delta texels for each target and vertex. */
    private static float[] pack(BufferGeometry geometry) {
        BufferAttribute positions = geometry.attribute(BufferGeometry.POSITION);
        if (positions == null) {
            throw new IllegalStateException("Morph geometry requires a position attribute");
        }
        int vertexCount = positions.count();
        float[] packed = new float
                [Math.multiplyExact(Math.multiplyExact(geometry.morphTargets().size(), vertexCount), 8)];
        int offset = 0;
        for (MorphTarget target : geometry.morphTargets()) {
            BufferAttribute targetPositions = target.positions();
            BufferAttribute targetNormals = target.normals().orElse(null);
            for (int vertex = 0; vertex < vertexCount; vertex++) {
                offset = copyVector(targetPositions, vertex, packed, offset);
                packed[offset++] = 0.0f;
                if (targetNormals == null) {
                    offset += 3;
                } else {
                    offset = copyVector(targetNormals, vertex, packed, offset);
                }
                packed[offset++] = 0.0f;
            }
        }
        return packed;
    }

    /** Copies one three-component attribute item into the packed destination. */
    private static int copyVector(BufferAttribute source, int item, float[] destination, int offset) {
        destination[offset++] = source.value(item, 0);
        destination[offset++] = source.value(item, 1);
        destination[offset++] = source.value(item, 2);
        return offset;
    }

    /** Captures attribute identities and versions after a complete upload. */
    private void rememberVersions(BufferGeometry geometry) {
        versions.clear();
        for (MorphTarget target : geometry.morphTargets()) {
            versions.add(new AttributeVersion(target.positions()));
            versions.add(new AttributeVersion(target.normals().orElse(null)));
        }
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
    public record UploadResult(int count, long byteCount) {}

    /** Identity and version snapshot without array-valued record components. */
    private static final class AttributeVersion {
        private final BufferAttribute attribute;
        private final long version;

        private AttributeVersion(BufferAttribute attribute) {
            this.attribute = attribute;
            version = attribute == null ? -1L : attribute.version();
        }

        private boolean matches(BufferAttribute candidate) {
            return candidate == attribute && (candidate == null || candidate.version() == version);
        }
    }
}
