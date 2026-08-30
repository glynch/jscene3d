/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Stable read-only view of GPU resources currently realized by a renderer. */
public final class ResourceStatistics {
    private int activeGeometryResources;
    private int programCount;

    ResourceStatistics() {
        // Resource counts intentionally begin at Java's zero-valued field defaults.
    }

    /** Returns currently realized geometry resources. */
    public int activeGeometryResources() {
        return activeGeometryResources;
    }

    /** Returns currently compiled shader programs. */
    public int programCount() {
        return programCount;
    }

    void setActiveGeometryResources(int activeGeometryResources) {
        this.activeGeometryResources = activeGeometryResources;
    }

    void setProgramCount(int programCount) {
        this.programCount = programCount;
    }
}
