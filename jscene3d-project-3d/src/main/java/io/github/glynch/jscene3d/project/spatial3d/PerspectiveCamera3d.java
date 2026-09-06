/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

/**
 * Mutable perspective projection state owned by one live entity.
 *
 * <p>The entity's {@link Transform3d} supplies position and orientation. Projection state and primary-camera
 * selection remain independent of renderer backend objects. This component is confined to its world's logical
 * simulation thread.
 */
public interface PerspectiveCamera3d extends AutoCloseable {
    /**
     * Returns the vertical field of view in degrees.
     *
     * @return current vertical field of view
     * @throws IllegalStateException if this component is closed
     */
    float fieldOfViewDegrees();

    /**
     * Returns the positive near clipping distance.
     *
     * @return current near clipping distance
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
     * Returns whether authored configuration selects this camera as primary.
     *
     * @return {@code true} when this is the world's selected authored camera
     * @throws IllegalStateException if this component is closed
     */
    boolean isPrimary();

    /**
     * Sets the vertical field of view.
     *
     * @param fieldOfViewDegrees finite value strictly between zero and 180 degrees
     * @throws IllegalArgumentException if the value is not finite or is outside the required range
     * @throws IllegalStateException if this component is closed
     */
    void setFieldOfViewDegrees(float fieldOfViewDegrees);

    /**
     * Atomically sets both clipping distances.
     *
     * @param near positive near clipping distance
     * @param far clipping distance greater than {@code near}
     * @throws IllegalArgumentException if either value is not finite or the required relationship is not satisfied
     * @throws IllegalStateException if this component is closed
     */
    void setClippingPlanes(float near, float far);

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
