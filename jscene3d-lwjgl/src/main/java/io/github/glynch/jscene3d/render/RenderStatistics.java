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
    private long renderedInstances;
    private int culledMeshes;
    private int visibleLines;
    private int culledLines;
    private int bufferUploads;
    private long bufferUploadBytes;
    private int textureUploads;
    private long textureUploadBytes;
    private int shadowMaps;
    private int shadowPasses;
    private int shadowDrawCalls;
    private long shadowTriangles;

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
     * Returns geometry instances submitted by visible mesh draws in the most recent frame.
     *
     * <p>An ordinary mesh contributes one. An instanced mesh contributes its active count.
     *
     * @return submitted mesh-instance count
     */
    public long renderedInstances() {
        return renderedInstances;
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

    /**
     * Returns shadow maps generated during the most recent frame.
     *
     * @return generated shadow-map count
     */
    public int shadowMaps() {
        return shadowMaps;
    }

    /**
     * Returns shadow depth passes generated during the most recent frame.
     *
     * @return generated shadow-pass count, including six passes per point light
     */
    public int shadowPasses() {
        return shadowPasses;
    }

    /**
     * Returns shadow-caster draw calls issued during the most recent frame.
     *
     * @return shadow draw-call count
     */
    public int shadowDrawCalls() {
        return shadowDrawCalls;
    }

    /**
     * Returns triangles submitted to shadow depth passes during the most recent frame.
     *
     * @return shadow triangle count
     */
    public long shadowTriangles() {
        return shadowTriangles;
    }

    /** Clears per-frame counters while retaining the completed-frame number. */
    void beginFrame() {
        drawCalls = 0;
        triangles = 0L;
        lineSegments = 0L;
        visibleMeshes = 0;
        renderedInstances = 0L;
        culledMeshes = 0;
        visibleLines = 0;
        culledLines = 0;
        bufferUploads = 0;
        bufferUploadBytes = 0L;
        textureUploads = 0;
        textureUploadBytes = 0L;
        shadowMaps = 0;
        shadowPasses = 0;
        shadowDrawCalls = 0;
        shadowTriangles = 0L;
    }

    /** Records successful completion of the current render call. */
    void completeFrame() {
        frame++;
    }

    /** Records one visible mesh draw and its submitted triangle count. */
    void recordMeshDraw(int elementCount) {
        recordMeshDraw(elementCount, 1);
    }

    /** Records one visible mesh batch and its repeated submitted triangle count. */
    void recordMeshDraw(int elementCount, int instanceCount) {
        drawCalls++;
        visibleMeshes++;
        renderedInstances += instanceCount;
        triangles += (elementCount / 3L) * instanceCount;
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

    /** Records aggregate shadow-map generation work for the current frame. */
    void recordShadowWork(int maps, int passes, int drawCalls, long triangles) {
        shadowMaps += maps;
        shadowPasses += passes;
        shadowDrawCalls += drawCalls;
        shadowTriangles += triangles;
    }
}
