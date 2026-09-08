/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.objects.BillboardAlignment;
import org.joml.Vector2fc;

/** Mutable per-instance presentation state for one shared-material camera-facing rectangle. */
public interface BillboardRenderer3d extends AutoCloseable {
    /**
     * Returns the retained immutable unlit material resource.
     *
     * @return shared billboard material
     */
    Material3dResource material();

    /**
     * Returns the stable live view of world-space width and height.
     *
     * @return billboard size
     */
    Vector2fc size();

    /**
     * Returns the stable live view of normalized anchor coordinates.
     *
     * @return normalized billboard anchor
     */
    Vector2fc anchor();

    /**
     * Returns the current camera-facing alignment mode.
     *
     * @return billboard alignment
     */
    BillboardAlignment alignment();

    /**
     * Returns local visibility retained across entity disablement.
     *
     * @return local visibility
     */
    boolean isVisible();

    /**
     * Changes local visibility without mutating the shared material.
     *
     * @param visible new local visibility
     */
    void setVisible(boolean visible);

    /**
     * Returns whether the owning world permanently released this component.
     *
     * @return {@code true} after release
     */
    boolean isClosed();

    /** Releases this component from its world-scoped 3D adapter; repeated calls have no effect. */
    @Override
    void close();
}
