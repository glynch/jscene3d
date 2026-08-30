/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Objects;

/** Unlit material with a base color and optional per-vertex color multiplication. */
public final class BasicMaterial extends Material {
    private Color color;
    private boolean usesVertexColors;

    /** Creates an opaque white basic material. */
    public BasicMaterial() {
        this(Color.WHITE);
    }

    /**
     * Creates an opaque basic material with the supplied color.
     *
     * @param color immutable linear-sRGB base color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public BasicMaterial(Color color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    /**
     * Returns the base color.
     *
     * @return the immutable linear-sRGB color
     * @throws IllegalStateException if this material is closed
     */
    public Color color() {
        requireOpen();
        return color;
    }

    /**
     * Changes the base color.
     *
     * @param color immutable linear-sRGB base color
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
