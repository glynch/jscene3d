/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Stable read-only view of GPU resources currently realized by a renderer. */
public final class ResourceStatistics {
    private int activeGeometryResources;
    private int activeTextureResources;
    private int activeInstanceResources;
    private int activeMorphResources;
    private int programCount;
    private int activeShadowMaps;

    /** Creates zero-valued resource statistics for one renderer. */
    ResourceStatistics() {
        // Resource counts intentionally begin at Java's zero-valued field defaults.
    }

    /**
     * Returns currently realized geometry resources.
     *
     * @return active geometry-resource count
     */
    public int activeGeometryResources() {
        return activeGeometryResources;
    }

    /**
     * Returns currently realized texture resources.
     *
     * @return active texture-resource count
     */
    public int activeTextureResources() {
        return activeTextureResources;
    }

    /**
     * Returns currently realized instanced-mesh data resources.
     *
     * @return active instance-resource count
     */
    public int activeInstanceResources() {
        return activeInstanceResources;
    }

    /**
     * Returns currently realized morph-target delta and weight resources.
     *
     * @return active morph-resource count
     */
    public int activeMorphResources() {
        return activeMorphResources;
    }

    /**
     * Returns currently compiled shader programs.
     *
     * @return compiled-program count
     */
    public int programCount() {
        return programCount;
    }

    /**
     * Returns retained per-light shadow maps.
     *
     * @return active shadow-map count
     */
    public int activeShadowMaps() {
        return activeShadowMaps;
    }

    /** Replaces the current context-local geometry-resource count. */
    void setActiveGeometryResources(int activeGeometryResources) {
        this.activeGeometryResources = activeGeometryResources;
    }

    /** Replaces the current context-local texture-resource count. */
    void setActiveTextureResources(int activeTextureResources) {
        this.activeTextureResources = activeTextureResources;
    }

    /** Replaces the current context-local instance-resource count. */
    void setActiveInstanceResources(int activeInstanceResources) {
        this.activeInstanceResources = activeInstanceResources;
    }

    /** Replaces the current context-local morph-resource count. */
    void setActiveMorphResources(int activeMorphResources) {
        this.activeMorphResources = activeMorphResources;
    }

    /** Replaces the current context-local shader-program count. */
    void setProgramCount(int programCount) {
        this.programCount = programCount;
    }

    /** Replaces the retained per-light shadow-map count. */
    void setActiveShadowMaps(int activeShadowMaps) {
        this.activeShadowMaps = activeShadowMaps;
    }
}
