/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

/**
 * Mutable orthographic projection state owned by one live entity.
 *
 * <p>The entity's {@link Transform3d} supplies position and orientation. Projection state and primary-camera
 * selection remain independent of renderer backend objects. This component is confined to its world's logical
 * simulation thread.
 */
public interface OrthographicCamera3d extends AutoCloseable {
    /**
     * Returns the left projection boundary.
     *
     * @return current finite left projection boundary
     * @throws IllegalStateException if this component is closed
     */
    float left();

    /**
     * Returns the right projection boundary.
     *
     * @return current finite right projection boundary
     * @throws IllegalStateException if this component is closed
     */
    float right();

    /**
     * Returns the top projection boundary.
     *
     * @return current finite top projection boundary
     * @throws IllegalStateException if this component is closed
     */
    float top();

    /**
     * Returns the bottom projection boundary.
     *
     * @return current finite bottom projection boundary
     * @throws IllegalStateException if this component is closed
     */
    float bottom();

    /**
     * Returns the near clipping distance.
     *
     * @return current non-negative near clipping distance
     * @throws IllegalStateException if this component is closed
     */
    float near();

    /**
     * Returns the far clipping distance.
     *
     * @return current far clipping distance
     * @throws IllegalStateException if this component is closed
     */
    float far();

    /**
     * Returns the projection magnification.
     *
     * @return current positive projection magnification
     * @throws IllegalStateException if this component is closed
     */
    float zoom();

    /**
     * Returns whether authored configuration selects this camera as primary.
     *
     * @return {@code true} when this is the world's selected authored camera
     * @throws IllegalStateException if this component is closed
     */
    boolean isPrimary();

    /**
     * Atomically sets all projection boundaries.
     *
     * @param left finite left projection boundary
     * @param right finite right projection boundary greater than {@code left}
     * @param top finite top projection boundary greater than {@code bottom}
     * @param bottom finite bottom projection boundary
     * @throws IllegalArgumentException if a value or required relationship is invalid
     * @throws IllegalStateException if this component is closed
     */
    void setBounds(float left, float right, float top, float bottom);

    /**
     * Atomically sets both clipping distances.
     *
     * @param near finite non-negative near clipping distance
     * @param far finite far clipping distance greater than {@code near}
     * @throws IllegalArgumentException if a value or required relationship is invalid
     * @throws IllegalStateException if this component is closed
     */
    void setClippingPlanes(float near, float far);

    /**
     * Sets projection magnification.
     *
     * @param zoom finite positive projection magnification
     * @throws IllegalArgumentException if {@code zoom} is invalid
     * @throws IllegalStateException if this component is closed
     */
    void setZoom(float zoom);

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
