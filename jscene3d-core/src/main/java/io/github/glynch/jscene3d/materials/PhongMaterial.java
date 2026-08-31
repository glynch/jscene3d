/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.textures.Texture;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Lit material combining diffuse illumination with Blinn-Phong specular highlights.
 *
 * <p>Rendering requires geometry normals. The base color can additionally be multiplied by vertex
 * colors and a color map. Emissive color is independent of scene lights, while specular color and
 * shininess control reflected highlights. Instances are mutable, shareable, and not thread-safe.
 */
public final class PhongMaterial extends Material {
    private static final float DEFAULT_SHININESS = 30.0f;
    private static final Color DEFAULT_SPECULAR = Color.srgb(0x111111);

    private Color color;
    private Color emissive;
    private float emissiveIntensity;
    private Color specular;
    private float shininess;
    private boolean usesVertexColors;
    private @Nullable Texture colorMap;

    /** Creates an opaque white Phong material with subdued specular reflection. */
    public PhongMaterial() {
        this(Color.WHITE);
    }

    /**
     * Creates an opaque Phong material with the supplied base color.
     *
     * @param color immutable linear-sRGB base color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public PhongMaterial(Color color) {
        this.color = Objects.requireNonNull(color, "color");
        emissive = Color.BLACK;
        emissiveIntensity = 1.0f;
        specular = DEFAULT_SPECULAR;
        shininess = DEFAULT_SHININESS;
    }

    /**
     * Returns the diffuse base color.
     *
     * @return immutable linear-sRGB color
     * @throws IllegalStateException if this material is closed
     */
    public Color color() {
        requireOpen();
        return color;
    }

    /**
     * Changes the diffuse base color.
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
     * Returns the emissive color added independently of scene lights.
     *
     * @return immutable linear-sRGB color, initially black
     * @throws IllegalStateException if this material is closed
     */
    public Color emissive() {
        requireOpen();
        return emissive;
    }

    /**
     * Changes the emissive color.
     *
     * @param emissive immutable linear-sRGB color
     * @throws NullPointerException if {@code emissive} is {@code null}
     * @throws IllegalStateException if this material is closed
     */
    public void setEmissive(Color emissive) {
        requireOpen();
        Color validEmissive = Objects.requireNonNull(emissive, "emissive");
        if (!this.emissive.equals(validEmissive)) {
            this.emissive = validEmissive;
            markChanged();
        }
    }

    /**
     * Returns the emissive color multiplier.
     *
     * @return non-negative multiplier, initially one
     * @throws IllegalStateException if this material is closed
     */
    public float emissiveIntensity() {
        requireOpen();
        return emissiveIntensity;
    }

    /**
     * Changes the emissive color multiplier.
     *
     * @param emissiveIntensity finite non-negative multiplier
     * @throws IllegalArgumentException if the value is negative or non-finite
     * @throws IllegalStateException if this material is closed
     */
    public void setEmissiveIntensity(float emissiveIntensity) {
        requireOpen();
        float validIntensity = Preconditions.requireNonNegative(emissiveIntensity, "emissiveIntensity");
        if (this.emissiveIntensity != validIntensity) {
            this.emissiveIntensity = validIntensity;
            markChanged();
        }
    }

    /**
     * Returns the specular highlight color.
     *
     * @return immutable linear-sRGB color, initially sRGB {@code #111111}
     * @throws IllegalStateException if this material is closed
     */
    public Color specular() {
        requireOpen();
        return specular;
    }

    /**
     * Changes the specular highlight color.
     *
     * @param specular immutable linear-sRGB color
     * @throws NullPointerException if {@code specular} is {@code null}
     * @throws IllegalStateException if this material is closed
     */
    public void setSpecular(Color specular) {
        requireOpen();
        Color validSpecular = Objects.requireNonNull(specular, "specular");
        if (!this.specular.equals(validSpecular)) {
            this.specular = validSpecular;
            markChanged();
        }
    }

    /**
     * Returns the specular highlight exponent.
     *
     * @return finite non-negative exponent, initially {@code 30}
     * @throws IllegalStateException if this material is closed
     */
    public float shininess() {
        requireOpen();
        return shininess;
    }

    /**
     * Changes the specular highlight exponent.
     *
     * @param shininess finite non-negative exponent
     * @throws IllegalArgumentException if the value is negative or non-finite
     * @throws IllegalStateException if this material is closed
     */
    public void setShininess(float shininess) {
        requireOpen();
        float validShininess = Preconditions.requireNonNegative(shininess, "shininess");
        if (this.shininess != validShininess) {
            this.shininess = validShininess;
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
     * Returns the optional base-color texture.
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
