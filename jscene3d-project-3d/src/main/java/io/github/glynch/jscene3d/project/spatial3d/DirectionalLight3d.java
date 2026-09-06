/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.math.Color;
import org.joml.Vector3fc;

/** Mutable instance state for one directional light attached to an entity's {@link Transform3d}. */
public interface DirectionalLight3d extends AutoCloseable {
    /**
     * Returns the immutable linear-sRGB light color.
     *
     * @return current light color
     * @throws IllegalStateException if this component is closed
     */
    Color color();

    /**
     * Returns the finite non-negative light intensity.
     *
     * @return current intensity
     * @throws IllegalStateException if this component is closed
     */
    float intensity();

    /**
     * Returns the stable live read-only world-space target.
     *
     * @return current target view
     * @throws IllegalStateException if this component is closed
     */
    Vector3fc target();

    /**
     * Sets the immutable linear-sRGB light color.
     *
     * @param color new light color
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalStateException if this component is closed
     */
    void setColor(Color color);

    /**
     * Sets the finite non-negative light intensity.
     *
     * @param intensity new intensity
     * @throws IllegalArgumentException if {@code intensity} is not finite and non-negative
     * @throws IllegalStateException if this component is closed
     */
    void setIntensity(float intensity);

    /**
     * Sets the finite world-space target.
     *
     * @param x target X coordinate
     * @param y target Y coordinate
     * @param z target Z coordinate
     * @throws IllegalArgumentException if any coordinate is not finite
     * @throws IllegalStateException if this component is closed
     */
    void setTarget(float x, float y, float z);

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
