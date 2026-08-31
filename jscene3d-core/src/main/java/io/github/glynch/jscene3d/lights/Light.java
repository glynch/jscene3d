/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.Objects;

/**
 * Base scene component for renderer-supported illumination.
 *
 * <p>Lights are mutable scene nodes and are not thread-safe. They own no external resources and do
 * not require closure. Their color is expressed in JScene3D's linear-sRGB working space, while
 * intensity is a practical linear multiplier rather than a physically calibrated unit.
 */
public abstract sealed class Light extends Object3D
        permits AmbientLight, DirectionalLight, HemisphereLight, PointLight, SpotLight {
    private Color color;
    private float intensity;

    /** Creates a white light with unit intensity. */
    protected Light() {
        this(Color.WHITE, 1.0f);
    }

    /**
     * Creates a light with unit intensity.
     *
     * @param color immutable linear-sRGB light color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    protected Light(Color color) {
        this(color, 1.0f);
    }

    /**
     * Creates a light with the supplied color and intensity.
     *
     * @param color immutable linear-sRGB light color
     * @param intensity finite non-negative intensity multiplier
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code intensity} is negative or non-finite
     */
    protected Light(Color color, float intensity) {
        this.color = Objects.requireNonNull(color, "color");
        this.intensity = Preconditions.requireNonNegative(intensity, "intensity");
    }

    /**
     * Returns the light color.
     *
     * @return immutable linear-sRGB color
     */
    public final Color color() {
        return color;
    }

    /**
     * Changes the light color.
     *
     * @param color immutable linear-sRGB light color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public final void setColor(Color color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    /**
     * Returns the practical linear intensity multiplier.
     *
     * @return finite non-negative intensity, initially one
     */
    public final float intensity() {
        return intensity;
    }

    /**
     * Changes the practical linear intensity multiplier.
     *
     * @param intensity finite non-negative intensity multiplier
     * @throws IllegalArgumentException if {@code intensity} is negative or non-finite
     */
    public final void setIntensity(float intensity) {
        this.intensity = Preconditions.requireNonNegative(intensity, "intensity");
    }
}
