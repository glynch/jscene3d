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
    private long lineSegments;
    private int visibleMeshes;
    private int culledMeshes;
    private int visibleLines;
    private int culledLines;
    private int bufferUploads;
    private long bufferUploadBytes;
    private int textureUploads;
    private long textureUploadBytes;

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
     * Returns line segments submitted by the most recent frame.
     *
     * @return submitted line-segment count
     */
    public long lineSegments() {
        return lineSegments;
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
     * Returns visible line objects drawn by the most recent frame.
     *
     * @return visible line-object count
     */
    public int visibleLines() {
        return visibleLines;
    }

    /**
     * Returns line objects rejected by frustum culling during the most recent frame.
     *
     * @return culled line-object count
     */
    public int culledLines() {
        return culledLines;
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

    /**
     * Returns texture image uploads performed during the most recent frame.
     *
     * @return texture-upload count
     */
    public int textureUploads() {
        return textureUploads;
    }

    /**
     * Returns texture image bytes uploaded during the most recent frame.
     *
     * @return uploaded texture byte count
     */
    public long textureUploadBytes() {
        return textureUploadBytes;
    }

    /** Clears per-frame counters while retaining the completed-frame number. */
    void beginFrame() {
        drawCalls = 0;
        triangles = 0L;
        lineSegments = 0L;
        visibleMeshes = 0;
        culledMeshes = 0;
        visibleLines = 0;
        culledLines = 0;
        bufferUploads = 0;
        bufferUploadBytes = 0L;
        textureUploads = 0;
        textureUploadBytes = 0L;
    }

    /** Records successful completion of the current render call. */
    void completeFrame() {
        frame++;
    }

    /** Records one visible mesh draw and its submitted triangle count. */
    void recordMeshDraw(int elementCount) {
        drawCalls++;
        visibleMeshes++;
        triangles += elementCount / 3L;
    }

    /** Records one visible line-object draw and its submitted segment count. */
    void recordLineDraw(long segmentCount) {
        drawCalls++;
        visibleLines++;
        lineSegments += segmentCount;
    }

    /** Records one mesh rejected by frustum culling. */
    void recordCulledMesh() {
        culledMeshes++;
    }

    /** Records one line object rejected by frustum culling. */
    void recordCulledLine() {
        culledLines++;
    }

    /** Records meshes rejected by frustum culling. */
    void recordCulledMeshes(int count) {
        culledMeshes += count;
    }

    /** Records line objects rejected by frustum culling. */
    void recordCulledLines(int count) {
        culledLines += count;
    }

    /** Records one GPU-buffer upload and its byte count. */
    void recordUpload(long byteCount) {
        bufferUploads++;
        bufferUploadBytes += byteCount;
    }

    /** Records several GPU-buffer uploads and their combined byte count. */
    void recordUploads(int count, long byteCount) {
        bufferUploads += count;
        bufferUploadBytes += byteCount;
    }

    /** Records one GPU texture-image upload and its byte count. */
    void recordTextureUpload(long byteCount) {
        textureUploads++;
        textureUploadBytes += byteCount;
    }
}
