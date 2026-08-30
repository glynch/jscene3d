/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Stable container for a renderer's focused diagnostic views. */
public final class RendererInfo {
    private final RenderStatistics statistics;
    private final ResourceStatistics resources;

    RendererInfo() {
        statistics = new RenderStatistics();
        resources = new ResourceStatistics();
    }

    /** Returns the stable, read-only view of most-recent-frame statistics. */
    public RenderStatistics statistics() {
        return statistics;
    }

    /** Returns the stable, read-only view of currently realized GPU resources. */
    public ResourceStatistics resources() {
        return resources;
    }
}
