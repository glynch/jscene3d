/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;

/**
 * Unlit line material with a base color and optional vertex-color multiplication.
 *
 * <p>Lines have no face orientation, so the inherited {@link MaterialSide} setting has no effect.
 * The portable raster width is one framebuffer pixel.
 */
public final class LineBasicMaterial extends Material {
    private Color color;
    private boolean usesVertexColors;

    /** Creates an opaque white line material. */
    public LineBasicMaterial() {
        this(Color.WHITE);
    }

    /**
     * Creates an opaque line material with the supplied color.
     *
     * @param color immutable linear-sRGB base color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public LineBasicMaterial(Color color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    /**
     * Returns the base color.
     *
     * @return immutable linear-sRGB color
     * @throws IllegalStateException if this material is closed
     */
    public Color color() {
        requireOpen();
        return color;
    }

    /**
     * Changes the base color.
     *
     * @param color immutable linear-sRGB color
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalStateException if this material is closed
     */
    public void setColor(Color color) {
        requireOpen();
        Color validColor = Objects.requireNonNull(color, "color");
        if (!this.color.equals(validColor)) {
            this.color = validColor;
            markChanged();
        }
    }

    /**
     * Returns whether the material multiplies its base color by vertex colors.
     *
     * @return {@code false} by default
     * @throws IllegalStateException if this material is closed
     */
    public boolean usesVertexColors() {
        requireOpen();
        return usesVertexColors;
    }

    /**
     * Changes whether the material multiplies its base color by vertex colors.
     *
     * @param enabled whether vertex colors are used
     * @throws IllegalStateException if this material is closed
     */
    public void setUsesVertexColors(boolean enabled) {
        requireOpen();
        if (usesVertexColors != enabled) {
            usesVertexColors = enabled;
            markChanged();
        }
    }
}
