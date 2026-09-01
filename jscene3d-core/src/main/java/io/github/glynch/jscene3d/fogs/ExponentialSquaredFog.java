/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.fogs;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;

/** Fog whose coverage grows exponentially with squared camera distance. */
public final class ExponentialSquaredFog implements Fog {
    private Color color;
    private float density;

    /**
     * Creates exponential-squared fog.
     *
     * @param color immutable linear-sRGB fog color
     * @param density finite non-negative density in inverse scene units
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code density} is negative or non-finite
     */
    public ExponentialSquaredFog(Color color, float density) {
        this.color = Objects.requireNonNull(color, "color");
        this.density = Preconditions.requireNonNegative(density, "density");
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    /**
     * Returns the fog density in inverse scene units.
     *
     * @return finite non-negative density
     */
    public float density() {
        return density;
    }

    /**
     * Changes the fog density.
     *
     * @param density finite non-negative density in inverse scene units
     * @throws IllegalArgumentException if {@code density} is negative or non-finite
     */
    public void setDensity(float density) {
        this.density = Preconditions.requireNonNegative(density, "density");
    }
}
