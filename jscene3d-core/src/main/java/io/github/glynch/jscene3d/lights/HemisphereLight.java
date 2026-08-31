/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;

/**
 * Ambient-like illumination interpolated between sky and ground colors by surface orientation.
 *
 * <p>The inherited {@link #color()} property is the sky color. The light's normalized world
 * position defines the direction toward the sky; the default position is {@code (0, 1, 0)}. A
 * zero world position cannot define that direction and fails during rendering. Hemisphere-light
 * shadows are not supported in version 0.1.
 */
public final class HemisphereLight extends Light {
    private Color groundColor;

    /** Creates a white sky and white ground light with unit intensity. */
    public HemisphereLight() {
        this(Color.WHITE, Color.WHITE, 1.0f);
    }

    /**
     * Creates a hemisphere light with unit intensity.
     *
     * @param skyColor immutable linear-sRGB sky color
     * @param groundColor immutable linear-sRGB ground color
     * @throws NullPointerException if a color is {@code null}
     */
    public HemisphereLight(Color skyColor, Color groundColor) {
        this(skyColor, groundColor, 1.0f);
    }

    /**
     * Creates a hemisphere light with the supplied colors and intensity.
     *
     * @param skyColor immutable linear-sRGB sky color
     * @param groundColor immutable linear-sRGB ground color
     * @param intensity finite non-negative intensity multiplier
     * @throws NullPointerException if a color is {@code null}
     * @throws IllegalArgumentException if {@code intensity} is negative or non-finite
     */
    public HemisphereLight(Color skyColor, Color groundColor, float intensity) {
        super(skyColor, intensity);
        this.groundColor = Objects.requireNonNull(groundColor, "groundColor");
        setPosition(0.0f, 1.0f, 0.0f);
    }

    /**
     * Returns the ground color.
     *
     * @return immutable linear-sRGB ground color
     */
    public Color groundColor() {
        return groundColor;
    }

    /**
     * Changes the ground color.
     *
     * @param groundColor immutable linear-sRGB ground color
     * @throws NullPointerException if {@code groundColor} is {@code null}
     */
    public void setGroundColor(Color groundColor) {
        this.groundColor = Objects.requireNonNull(groundColor, "groundColor");
    }
}
