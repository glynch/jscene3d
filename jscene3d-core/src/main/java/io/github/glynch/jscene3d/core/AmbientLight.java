/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/**
 * Direction-independent illumination applied uniformly to lit surfaces.
 *
 * <p>An ambient light participates in scene hierarchy and visibility traversal, but its position,
 * orientation, and scale do not affect its illumination.
 */
public final class AmbientLight extends Light {
    /** Creates a white ambient light with unit intensity. */
    public AmbientLight() {
        super();
    }

    /**
     * Creates an ambient light with unit intensity.
     *
     * @param color immutable linear-sRGB light color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public AmbientLight(Color color) {
        super(color);
    }

    /**
     * Creates an ambient light with the supplied color and intensity.
     *
     * @param color immutable linear-sRGB light color
     * @param intensity finite non-negative intensity multiplier
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code intensity} is negative or non-finite
     */
    public AmbientLight(Color color, float intensity) {
        super(color, intensity);
    }
}
