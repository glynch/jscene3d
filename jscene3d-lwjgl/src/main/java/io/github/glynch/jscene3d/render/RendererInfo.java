/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Stable read-only view of renderer resource and most-recent-frame statistics. */
public final class RendererInfo {
    private long frame;
    private int drawCalls;
    private long triangles;
    private int visibleMeshes;
    private int activeGeometryResources;
    private int programCount;
    private int bufferUploads;
    private long bufferUploadBytes;

    RendererInfo() {
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

    /** Returns currently realized geometry resources. */
    public int activeGeometryResources() {
        return activeGeometryResources;
    }

    /** Returns currently compiled shader programs. */
    public int programCount() {
        return programCount;
    }

    /** Returns buffer uploads performed by the most recent frame. */
    public int bufferUploads() {
        return bufferUploads;
    }

    /** Returns buffer bytes uploaded by the most recent frame. */
    public long bufferUploadBytes() {
        return bufferUploadBytes;
    }

    void beginFrame() {
        drawCalls = 0;
        triangles = 0L;
        visibleMeshes = 0;
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

    void recordUpload(long byteCount) {
        bufferUploads++;
        bufferUploadBytes += byteCount;
    }

    void setActiveGeometryResources(int activeGeometryResources) {
        this.activeGeometryResources = activeGeometryResources;
    }

    void setProgramCount(int programCount) {
        this.programCount = programCount;
    }
}
