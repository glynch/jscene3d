/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

/**
 * Mutable per-instance presentation state for one shared mesh and material pair.
 *
 * <p>Visibility is local instance state. Effective presentation additionally follows the owning entity's lifecycle.
 */
public interface MeshRenderer3d extends AutoCloseable {
    /**
     * Returns the retained immutable mesh resource.
     *
     * @return shared mesh resource
     * @throws IllegalStateException if this component is closed
     */
    Mesh3dResource mesh();

    /**
     * Returns the retained immutable material resource.
     *
     * @return shared material resource
     * @throws IllegalStateException if this component is closed
     */
    Material3dResource material();

    /**
     * Returns the local visibility retained across entity disablement.
     *
     * @return current local visibility
     * @throws IllegalStateException if this component is closed
     */
    boolean isVisible();

    /**
     * Changes local visibility without mutating either shared resource.
     *
     * @param visible new local visibility
     * @throws IllegalStateException if this component is closed
     */
    void setVisible(boolean visible);

    /**
     * Returns whether the owning world has permanently released this component.
     *
     * @return {@code true} after terminal closure
     */
    boolean isClosed();

    /** Releases this component from its world-scoped 3D adapter; repeated calls have no effect. */
    @Override
    void close();
}
