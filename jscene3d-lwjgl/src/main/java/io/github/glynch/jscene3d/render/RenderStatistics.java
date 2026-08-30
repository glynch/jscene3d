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

    /** Creates zero-valued frame statistics for one renderer. */
    RenderStatistics() {
        // Statistics intentionally begin at Java's zero-valued field defaults.
    }

    /**
     * Returns the number of completed render calls.
     *
     * @return monotonically increasing completed-frame count
     */
    public long frame() {
        return frame;
    }

    /**
     * Returns draw calls issued by the most recent frame.
     *
     * @return draw-call count
     */
    public int drawCalls() {
        return drawCalls;
    }

    /**
     * Returns triangles submitted by the most recent frame.
     *
     * @return submitted-triangle count
     */
    public long triangles() {
        return triangles;
    }

    /**
     * Returns visible meshes drawn by the most recent frame.
     *
     * @return visible-mesh count
     */
    public int visibleMeshes() {
        return visibleMeshes;
    }

    /**
     * Returns meshes rejected by frustum culling during the most recent frame.
     *
     * @return culled-mesh count
     */
    public int culledMeshes() {
        return culledMeshes;
    }

    /**
     * Returns buffer uploads performed during the most recent frame.
     *
     * @return buffer-upload count
     */
    public int bufferUploads() {
        return bufferUploads;
    }

    /**
     * Returns buffer bytes uploaded during the most recent frame.
     *
     * @return uploaded byte count
     */
    public long bufferUploadBytes() {
        return bufferUploadBytes;
    }

    /** Clears per-frame counters while retaining the completed-frame number. */
    void beginFrame() {
        drawCalls = 0;
        triangles = 0L;
        visibleMeshes = 0;
        culledMeshes = 0;
        bufferUploads = 0;
        bufferUploadBytes = 0L;
    }

    /** Records successful completion of the current render call. */
    void completeFrame() {
        frame++;
    }

    /** Records one visible mesh draw and its submitted triangle count. */
    void recordDraw(int elementCount) {
        drawCalls++;
        visibleMeshes++;
        triangles += elementCount / 3L;
    }

    /** Records one mesh rejected by frustum culling. */
    void recordCulledMesh() {
        culledMeshes++;
    }

    /** Records one GPU-buffer upload and its byte count. */
    void recordUpload(long byteCount) {
        bufferUploads++;
        bufferUploadBytes += byteCount;
    }
}
