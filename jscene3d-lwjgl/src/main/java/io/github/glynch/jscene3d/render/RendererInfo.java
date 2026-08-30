/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Stable container for a renderer's focused diagnostic views. */
public final class RendererInfo {
    private final RenderStatistics statistics;
    private final ResourceStatistics resources;

    /** Creates the stable diagnostic views owned by one renderer. */
    RendererInfo() {
        statistics = new RenderStatistics();
        resources = new ResourceStatistics();
    }

    /**
     * Returns the stable, read-only view of most-recent-frame statistics.
     *
     * @return frame statistics updated in place by the owning renderer
     */
    public RenderStatistics statistics() {
        return statistics;
    }

    /**
     * Returns the stable, read-only view of currently realized GPU resources.
     *
     * @return resource statistics updated in place by the owning renderer
     */
    public ResourceStatistics resources() {
        return resources;
    }
}
