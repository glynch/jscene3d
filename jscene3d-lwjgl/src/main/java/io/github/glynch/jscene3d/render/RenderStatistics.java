/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Stable read-only view of statistics recorded for the most recently completed render. */
public final class RenderStatistics {
    private long frame;
    private int drawCalls;
    private long triangles;
    private int visibleMeshes;
    private int culledMeshes;
    private int bufferUploads;
    private long bufferUploadBytes;

    RenderStatistics() {
        // Statistics intentionally begin at Java's zero-valued field defaults.
    }

    /** Returns the number of completed render calls. */
    public long frame() {
        return frame;
    }

    /** Returns draw calls issued by the most recent frame. */
    public int drawCalls() {
        return drawCalls;
    }

    /** Returns triangles submitted by the most recent frame. */
    public long triangles() {
        return triangles;
    }

    /** Returns visible meshes drawn by the most recent frame. */
    public int visibleMeshes() {
        return visibleMeshes;
    }

    /** Returns meshes rejected by frustum culling during the most recent frame. */
    public int culledMeshes() {
        return culledMeshes;
    }

    /** Returns buffer uploads performed during the most recent frame. */
    public int bufferUploads() {
        return bufferUploads;
    }

    /** Returns buffer bytes uploaded during the most recent frame. */
    public long bufferUploadBytes() {
        return bufferUploadBytes;
    }

    void beginFrame() {
        drawCalls = 0;
        triangles = 0L;
        visibleMeshes = 0;
        culledMeshes = 0;
        bufferUploads = 0;
        bufferUploadBytes = 0L;
    }

    void completeFrame() {
        frame++;
    }

    void recordDraw(int elementCount) {
        drawCalls++;
        visibleMeshes++;
        triangles += elementCount / 3L;
    }

    void recordCulledMesh() {
        culledMeshes++;
    }

    void recordUpload(long byteCount) {
        bufferUploads++;
        bufferUploadBytes += byteCount;
    }
}
