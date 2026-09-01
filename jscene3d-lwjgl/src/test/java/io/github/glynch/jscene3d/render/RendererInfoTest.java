/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class RendererInfoTest {
    @Test
    void separatesFrameAndResourceStatistics() {
        RendererInfo info = new RendererInfo();

        RenderStatistics statistics = info.statistics();
        ResourceStatistics resources = info.resources();

        assertThat(info.statistics()).isSameAs(statistics);
        assertThat(info.resources()).isSameAs(resources);
        assertThat(statistics.frame()).isZero();
        assertThat(statistics.drawCalls()).isZero();
        assertThat(statistics.triangles()).isZero();
        assertThat(statistics.lineSegments()).isZero();
        assertThat(statistics.visibleMeshes()).isZero();
        assertThat(statistics.renderedInstances()).isZero();
        assertThat(statistics.culledMeshes()).isZero();
        assertThat(statistics.visibleLines()).isZero();
        assertThat(statistics.culledLines()).isZero();
        assertThat(statistics.bufferUploads()).isZero();
        assertThat(statistics.bufferUploadBytes()).isZero();
        assertThat(statistics.textureUploads()).isZero();
        assertThat(statistics.textureUploadBytes()).isZero();
        assertThat(statistics.shadowMaps()).isZero();
        assertThat(statistics.shadowPasses()).isZero();
        assertThat(statistics.shadowDrawCalls()).isZero();
        assertThat(statistics.shadowTriangles()).isZero();
        assertThat(resources.activeGeometryResources()).isZero();
        assertThat(resources.activeTextureResources()).isZero();
        assertThat(resources.activeInstanceResources()).isZero();
        assertThat(resources.programCount()).isZero();
        assertThat(resources.activeShadowMaps()).isZero();
    }

    @Test
    void resettingAFrameDoesNotResetResourceStatistics() {
        RendererInfo info = new RendererInfo();
        RenderStatistics statistics = info.statistics();
        ResourceStatistics resources = info.resources();
        statistics.recordMeshDraw(6, 4);
        statistics.recordLineDraw(3L);
        statistics.recordCulledMesh();
        statistics.recordCulledLine();
        statistics.recordUpload(24L);
        statistics.recordTextureUpload(16L);
        statistics.recordShadowWork(1, 6, 12, 24L);
        statistics.completeFrame();
        resources.setActiveGeometryResources(2);
        resources.setActiveTextureResources(3);
        resources.setActiveInstanceResources(5);
        resources.setProgramCount(1);
        resources.setActiveShadowMaps(4);

        statistics.beginFrame();

        assertThat(statistics.frame()).isEqualTo(1L);
        assertThat(statistics.drawCalls()).isZero();
        assertThat(statistics.triangles()).isZero();
        assertThat(statistics.lineSegments()).isZero();
        assertThat(statistics.visibleMeshes()).isZero();
        assertThat(statistics.renderedInstances()).isZero();
        assertThat(statistics.culledMeshes()).isZero();
        assertThat(statistics.visibleLines()).isZero();
        assertThat(statistics.culledLines()).isZero();
        assertThat(statistics.bufferUploads()).isZero();
        assertThat(statistics.bufferUploadBytes()).isZero();
        assertThat(statistics.textureUploads()).isZero();
        assertThat(statistics.textureUploadBytes()).isZero();
        assertThat(statistics.shadowMaps()).isZero();
        assertThat(statistics.shadowPasses()).isZero();
        assertThat(statistics.shadowDrawCalls()).isZero();
        assertThat(statistics.shadowTriangles()).isZero();
        assertThat(resources.activeGeometryResources()).isEqualTo(2);
        assertThat(resources.activeTextureResources()).isEqualTo(3);
        assertThat(resources.activeInstanceResources()).isEqualTo(5);
        assertThat(resources.programCount()).isEqualTo(1);
        assertThat(resources.activeShadowMaps()).isEqualTo(4);
    }
}
