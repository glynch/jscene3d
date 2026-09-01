/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.fogs;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;

/** Fog that increases smoothly between explicit near and far camera distances. */
public final class LinearFog implements Fog {
    private Color color;
    private float nearDistance;
    private float farDistance;

    /**
     * Creates linear fog with explicit clear and fully fogged distances.
     *
     * @param color immutable linear-sRGB fog color
     * @param nearDistance non-negative distance before which fog has no effect
     * @param farDistance distance after which the fog color completely replaces surface color
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if a distance is non-finite, the near distance is negative,
     *     or the far distance is not greater than the near distance
     */
    public LinearFog(Color color, float nearDistance, float farDistance) {
        this.color = Objects.requireNonNull(color, "color");
        this.nearDistance = Preconditions.requireNonNegative(nearDistance, "nearDistance");
        Preconditions.requireLessThan(this.nearDistance, "nearDistance", farDistance, "farDistance");
        this.farDistance = farDistance;
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
     * Returns the distance before which fog has no effect.
     *
     * @return finite non-negative near distance
     */
    public float nearDistance() {
        return nearDistance;
    }

    /**
     * Changes the distance before which fog has no effect.
     *
     * @param nearDistance finite non-negative distance less than the current far distance
     * @throws IllegalArgumentException if the value is invalid
     */
    public void setNearDistance(float nearDistance) {
        float validDistance = Preconditions.requireNonNegative(nearDistance, "nearDistance");
        Preconditions.requireLessThan(validDistance, "nearDistance", farDistance, "farDistance");
        this.nearDistance = validDistance;
    }

    /**
     * Returns the distance after which surface color is completely replaced by the fog color.
     *
     * @return finite far distance greater than the near distance
     */
    public float farDistance() {
        return farDistance;
    }

    /**
     * Changes the distance after which surface color is completely replaced by the fog color.
     *
     * @param farDistance finite distance greater than the current near distance
     * @throws IllegalArgumentException if the value is invalid
     */
    public void setFarDistance(float farDistance) {
        Preconditions.requireLessThan(nearDistance, "nearDistance", farDistance, "farDistance");
        this.farDistance = farDistance;
    }
}
