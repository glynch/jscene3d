/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/**
 * Omnidirectional light emitted from this scene node's world position.
 *
 * <p>Intensity attenuates according to the configured decay. A distance of zero leaves the light
 * unbounded; a positive distance establishes its maximum influence. Point-light shadows are not
 * supported in version 0.1.
 */
public final class PointLight extends Light {
    private float distance;
    private float decay;

    /** Creates an unbounded white point light with unit intensity and inverse-square decay. */
    public PointLight() {
        super();
        decay = 2.0f;
    }

    /**
     * Creates an unbounded point light with unit intensity and inverse-square decay.
     *
     * @param color immutable linear-sRGB light color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public PointLight(Color color) {
        super(color);
        decay = 2.0f;
    }

    /**
     * Creates an unbounded point light with the supplied intensity and inverse-square decay.
     *
     * @param color immutable linear-sRGB light color
     * @param intensity finite non-negative intensity multiplier
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code intensity} is negative or non-finite
     */
    public PointLight(Color color, float intensity) {
        super(color, intensity);
        decay = 2.0f;
    }

    /**
     * Returns the maximum influence distance.
     *
     * @return zero for unlimited influence, or a positive scene-unit distance
     */
    public float distance() {
        return distance;
    }

    /**
     * Changes the maximum influence distance.
     *
     * @param distance zero for unlimited influence, or a positive scene-unit distance
     * @throws IllegalArgumentException if {@code distance} is negative or non-finite
     */
    public void setDistance(float distance) {
        this.distance = Preconditions.requireNonNegative(distance, "distance");
    }

    /**
     * Returns the distance-attenuation exponent.
     *
     * @return finite non-negative exponent, initially two
     */
    public float decay() {
        return decay;
    }

    /**
     * Changes the distance-attenuation exponent.
     *
     * @param decay finite non-negative attenuation exponent
     * @throws IllegalArgumentException if {@code decay} is negative or non-finite
     */
    public void setDecay(float decay) {
        this.decay = Preconditions.requireNonNegative(decay, "decay");
    }
}
