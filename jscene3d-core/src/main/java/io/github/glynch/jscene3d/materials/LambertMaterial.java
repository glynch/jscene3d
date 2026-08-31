/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.textures.Texture;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Diffuse lit material with optional vertex-color and color-map multiplication.
 *
 * <p>Rendering requires geometry normals and at least one visible supported light to produce a
 * non-black result. Ambient and diffuse point-light contributions are evaluated in linear space.
 * A selected color map additionally requires texture coordinates. Instances are mutable,
 * shareable, and not thread-safe.
 */
public final class LambertMaterial extends Material {
    private Color color;
    private boolean usesVertexColors;
    private @Nullable Texture colorMap;

    /** Creates an opaque white Lambert material. */
    public LambertMaterial() {
        this(Color.WHITE);
    }

    /**
     * Creates an opaque Lambert material with the supplied base color.
     *
     * @param color immutable linear-sRGB base color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public LambertMaterial(Color color) {
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

    /**
     * Returns the optional base-color texture multiplied with the material and vertex colors.
     *
     * @return shared texture, or an empty value when no color map is selected
     * @throws IllegalStateException if this material is closed
     */
    public Optional<Texture> colorMap() {
        requireOpen();
        return Optional.ofNullable(colorMap);
    }

    /**
     * Selects a shared base-color texture without transferring ownership.
     *
     * @param colorMap open texture to sample
     * @throws NullPointerException if {@code colorMap} is {@code null}
     * @throws IllegalArgumentException if {@code colorMap} is closed
     * @throws IllegalStateException if this material is closed
     */
    public void setColorMap(Texture colorMap) {
        requireOpen();
        Texture validColorMap = Objects.requireNonNull(colorMap, "colorMap");
        if (validColorMap.isClosed()) {
            throw new IllegalArgumentException("colorMap must be open");
        }
        if (this.colorMap != validColorMap) {
            this.colorMap = validColorMap;
            markChanged();
        }
    }

    /**
     * Removes the selected base-color texture without closing it.
     *
     * @throws IllegalStateException if this material is closed
     */
    public void clearColorMap() {
        requireOpen();
        if (colorMap != null) {
            colorMap = null;
            markChanged();
        }
    }
}
