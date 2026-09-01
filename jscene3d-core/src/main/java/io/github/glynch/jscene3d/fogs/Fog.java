/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.fogs;

import io.github.glynch.jscene3d.math.Color;

/**
 * Scene-wide distance fog supported by the built-in renderer materials.
 *
 * <p>Fog descriptions are mutable and may be shared by multiple scenes. Custom shader materials
 * are responsible for implementing their own fog calculations.
 */
public sealed interface Fog permits ExponentialSquaredFog, LinearFog {
    /**
     * Returns the color approached as fog coverage increases.
     *
     * @return immutable linear-sRGB fog color
     */
    Color color();

    /**
     * Changes the color approached as fog coverage increases.
     *
     * @param color immutable linear-sRGB fog color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    void setColor(Color color);
}
