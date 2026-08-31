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
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

/**
 * Metallic-roughness physically based surface material.
 *
 * <p>The base color is multiplied by optional vertex colors and a color map. Metalness and
 * roughness may be modulated by the blue and green channels of a shared data texture. Normal,
 * occlusion, and emissive maps use the geometry's primary texture coordinates. Textures are shared
 * without ownership transfer. Instances are mutable, shareable, and not thread-safe.
 */
public final class StandardMaterial extends Material {
    private static final float DEFAULT_METALNESS = 0.0f;
    private static final float DEFAULT_ROUGHNESS = 1.0f;

    private final Vector2f normalScale;

    private Color color;
    private float metalness;
    private float roughness;
    private Color emissive;
    private float emissiveIntensity;
    private float occlusionStrength;
    private boolean usesVertexColors;
    private @Nullable Texture colorMap;
    private @Nullable Texture metalnessRoughnessMap;
    private @Nullable Texture normalMap;
    private @Nullable Texture occlusionMap;
    private @Nullable Texture emissiveMap;

    /** Creates an opaque white dielectric material with maximum roughness. */
    public StandardMaterial() {
        this(Color.WHITE);
    }

    /**
     * Creates a dielectric material with the supplied base color and maximum roughness.
     *
     * @param color immutable linear-sRGB base color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public StandardMaterial(Color color) {
        this.color = Objects.requireNonNull(color, "color");
        metalness = DEFAULT_METALNESS;
        roughness = DEFAULT_ROUGHNESS;
        emissive = Color.BLACK;
        emissiveIntensity = 1.0f;
        occlusionStrength = 1.0f;
        normalScale = new Vector2f(1.0f);
    }

    /**
     * Returns the immutable linear-sRGB base color.
     *
     * @return the base color, initially white unless supplied to the constructor
     * @throws IllegalStateException if this material is closed
     */
    public Color color() {
        requireOpen();
        return color;
    }

    /**
     * Changes the immutable linear-sRGB base color.
     *
     * @param color linear-sRGB base color
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
     * Returns the metallic contribution.
     *
     * @return value in the inclusive range {@code [0, 1]}, initially zero
     * @throws IllegalStateException if this material is closed
     */
    public float metalness() {
        requireOpen();
        return metalness;
    }

    /**
     * Changes the metallic contribution.
     *
     * @param metalness finite value in the inclusive range {@code [0, 1]}
     * @throws IllegalArgumentException if {@code metalness} is non-finite or outside its valid range
     * @throws IllegalStateException if this material is closed
     */
    public void setMetalness(float metalness) {
        requireOpen();
        float validMetalness = Preconditions.requireInRange(metalness, 0.0f, 1.0f, "metalness");
        if (this.metalness != validMetalness) {
            this.metalness = validMetalness;
            markChanged();
        }
    }

    /**
     * Returns perceptual surface roughness.
     *
     * @return value in the inclusive range {@code [0, 1]}, initially one
     * @throws IllegalStateException if this material is closed
     */
    public float roughness() {
        requireOpen();
        return roughness;
    }

    /**
     * Changes perceptual surface roughness.
     *
     * @param roughness finite value in the inclusive range {@code [0, 1]}
     * @throws IllegalArgumentException if {@code roughness} is non-finite or outside its valid range
     * @throws IllegalStateException if this material is closed
     */
    public void setRoughness(float roughness) {
        requireOpen();
        float validRoughness = Preconditions.requireInRange(roughness, 0.0f, 1.0f, "roughness");
        if (this.roughness != validRoughness) {
            this.roughness = validRoughness;
            markChanged();
        }
    }

    /**
     * Returns the immutable linear-sRGB emissive color.
     *
     * @return the emissive color, initially black
     * @throws IllegalStateException if this material is closed
     */
    public Color emissive() {
        requireOpen();
        return emissive;
    }

    /**
     * Changes the immutable linear-sRGB emissive color.
     *
     * @param emissive linear-sRGB emissive color
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
     * Returns the emissive multiplier.
     *
     * @return a finite non-negative value, initially one
     * @throws IllegalStateException if this material is closed
     */
    public float emissiveIntensity() {
        requireOpen();
        return emissiveIntensity;
    }

    /**
     * Changes the emissive multiplier.
     *
     * @param emissiveIntensity finite non-negative multiplier
     * @throws IllegalArgumentException if {@code emissiveIntensity} is negative or non-finite
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
     * Returns the ambient-occlusion map strength.
     *
     * @return value in the inclusive range {@code [0, 1]}, initially one
     * @throws IllegalStateException if this material is closed
     */
    public float occlusionStrength() {
        requireOpen();
        return occlusionStrength;
    }

    /**
     * Changes the ambient-occlusion map strength.
     *
     * @param occlusionStrength finite value in the inclusive range {@code [0, 1]}
     * @throws IllegalArgumentException if the value is non-finite or outside its valid range
     * @throws IllegalStateException if this material is closed
     */
    public void setOcclusionStrength(float occlusionStrength) {
        requireOpen();
        float validStrength = Preconditions.requireInRange(occlusionStrength, 0.0f, 1.0f, "occlusionStrength");
        if (this.occlusionStrength != validStrength) {
            this.occlusionStrength = validStrength;
            markChanged();
        }
    }

    /**
     * Copies the tangent-space normal-map scale into {@code destination}.
     *
     * @param destination vector that receives the current scale
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     * @throws IllegalStateException if this material is closed
     */
    public Vector2f normalScale(Vector2f destination) {
        requireOpen();
        return Objects.requireNonNull(destination, "destination").set(normalScale);
    }

    /**
     * Changes the tangent-space normal-map scale.
     *
     * @param x finite scale applied to the tangent component
     * @param y finite scale applied to the bitangent component
     * @throws IllegalArgumentException if either value is non-finite
     * @throws IllegalStateException if this material is closed
     */
    public void setNormalScale(float x, float y) {
        requireOpen();
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        if (!normalScale.equals(validX, validY)) {
            normalScale.set(validX, validY);
            markChanged();
        }
    }

    /**
     * Copies an existing value into the tangent-space normal-map scale.
     *
     * @param normalScale finite tangent and bitangent scale
     * @throws NullPointerException if {@code normalScale} is {@code null}
     * @throws IllegalArgumentException if either component is non-finite
     * @throws IllegalStateException if this material is closed
     */
    public void setNormalScale(Vector2fc normalScale) {
        Vector2fc validScale = Preconditions.requireFinite(normalScale, "normalScale");
        setNormalScale(validScale.x(), validScale.y());
    }

    /**
     * Returns whether vertex colors multiply the base color.
     *
     * @return {@code false} by default
     * @throws IllegalStateException if this material is closed
     */
    public boolean usesVertexColors() {
        requireOpen();
        return usesVertexColors;
    }

    /**
     * Changes whether vertex colors multiply the base color.
     *
     * @param enabled whether to multiply the base color by geometry vertex colors
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
     * Returns the optional sRGB base-color texture.
     *
     * @return the selected shared texture, or an empty optional
     * @throws IllegalStateException if this material is closed
     */
    public Optional<Texture> colorMap() {
        requireOpen();
        return Optional.ofNullable(colorMap);
    }

    /**
     * Selects a shared open sRGB base-color texture.
     *
     * @param colorMap open texture to sample using the primary texture coordinates
     * @throws NullPointerException if {@code colorMap} is {@code null}
     * @throws IllegalArgumentException if {@code colorMap} is closed
     * @throws IllegalStateException if this material is closed
     */
    public void setColorMap(Texture colorMap) {
        requireOpen();
        Texture validMap = Preconditions.requireOpen(colorMap, "colorMap");
        if (this.colorMap != validMap) {
            this.colorMap = validMap;
            markChanged();
        }
    }

    /** Removes the base-color texture without closing it. */
    public void clearColorMap() {
        requireOpen();
        if (colorMap != null) {
            colorMap = null;
            markChanged();
        }
    }

    /**
     * Returns the optional linear metallic-roughness texture.
     *
     * @return the selected shared texture, or an empty optional
     * @throws IllegalStateException if this material is closed
     */
    public Optional<Texture> metalnessRoughnessMap() {
        requireOpen();
        return Optional.ofNullable(metalnessRoughnessMap);
    }

    /**
     * Selects a shared open linear metallic-roughness texture.
     *
     * <p>The green channel modulates roughness and the blue channel modulates metalness.
     *
     * @param metalnessRoughnessMap open texture to sample using the primary texture coordinates
     * @throws NullPointerException if {@code metalnessRoughnessMap} is {@code null}
     * @throws IllegalArgumentException if {@code metalnessRoughnessMap} is closed
     * @throws IllegalStateException if this material is closed
     */
    public void setMetalnessRoughnessMap(Texture metalnessRoughnessMap) {
        requireOpen();
        Texture validMap = Preconditions.requireOpen(metalnessRoughnessMap, "metalnessRoughnessMap");
        if (this.metalnessRoughnessMap != validMap) {
            this.metalnessRoughnessMap = validMap;
            markChanged();
        }
    }

    /** Removes the metallic-roughness texture without closing it. */
    public void clearMetalnessRoughnessMap() {
        requireOpen();
        if (metalnessRoughnessMap != null) {
            metalnessRoughnessMap = null;
            markChanged();
        }
    }

    /**
     * Returns the optional linear tangent-space normal texture.
     *
     * @return the selected shared texture, or an empty optional
     * @throws IllegalStateException if this material is closed
     */
    public Optional<Texture> normalMap() {
        requireOpen();
        return Optional.ofNullable(normalMap);
    }

    /**
     * Selects a shared open linear tangent-space normal texture.
     *
     * @param normalMap open texture to sample using the primary texture coordinates
     * @throws NullPointerException if {@code normalMap} is {@code null}
     * @throws IllegalArgumentException if {@code normalMap} is closed
     * @throws IllegalStateException if this material is closed
     */
    public void setNormalMap(Texture normalMap) {
        requireOpen();
        Texture validMap = Preconditions.requireOpen(normalMap, "normalMap");
        if (this.normalMap != validMap) {
            this.normalMap = validMap;
            markChanged();
        }
    }

    /** Removes the tangent-space normal texture without closing it. */
    public void clearNormalMap() {
        requireOpen();
        if (normalMap != null) {
            normalMap = null;
            markChanged();
        }
    }

    /**
     * Returns the optional linear ambient-occlusion texture.
     *
     * @return the selected shared texture, or an empty optional
     * @throws IllegalStateException if this material is closed
     */
    public Optional<Texture> occlusionMap() {
        requireOpen();
        return Optional.ofNullable(occlusionMap);
    }

    /**
     * Selects a shared open linear ambient-occlusion texture.
     *
     * <p>The red channel modulates indirect lighting.
     *
     * @param occlusionMap open texture to sample using the primary texture coordinates
     * @throws NullPointerException if {@code occlusionMap} is {@code null}
     * @throws IllegalArgumentException if {@code occlusionMap} is closed
     * @throws IllegalStateException if this material is closed
     */
    public void setOcclusionMap(Texture occlusionMap) {
        requireOpen();
        Texture validMap = Preconditions.requireOpen(occlusionMap, "occlusionMap");
        if (this.occlusionMap != validMap) {
            this.occlusionMap = validMap;
            markChanged();
        }
    }

    /** Removes the ambient-occlusion texture without closing it. */
    public void clearOcclusionMap() {
        requireOpen();
        if (occlusionMap != null) {
            occlusionMap = null;
            markChanged();
        }
    }

    /**
     * Returns the optional sRGB emissive texture.
     *
     * @return the selected shared texture, or an empty optional
     * @throws IllegalStateException if this material is closed
     */
    public Optional<Texture> emissiveMap() {
        requireOpen();
        return Optional.ofNullable(emissiveMap);
    }

    /**
     * Selects a shared open sRGB emissive texture.
     *
     * @param emissiveMap open texture to sample using the primary texture coordinates
     * @throws NullPointerException if {@code emissiveMap} is {@code null}
     * @throws IllegalArgumentException if {@code emissiveMap} is closed
     * @throws IllegalStateException if this material is closed
     */
    public void setEmissiveMap(Texture emissiveMap) {
        requireOpen();
        Texture validMap = Preconditions.requireOpen(emissiveMap, "emissiveMap");
        if (this.emissiveMap != validMap) {
            this.emissiveMap = validMap;
            markChanged();
        }
    }

    /** Removes the emissive texture without closing it. */
    public void clearEmissiveMap() {
        requireOpen();
        if (emissiveMap != null) {
            emissiveMap = null;
            markChanged();
        }
    }
}
