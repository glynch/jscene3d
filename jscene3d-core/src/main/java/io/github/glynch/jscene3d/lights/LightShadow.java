/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import io.github.glynch.jscene3d.internal.Preconditions;

/**
 * Renderer-independent shadow-map and shadow-camera configuration owned by one light.
 *
 * <p>The owning light retains this mutable description for its lifetime. Map dimensions are
 * requested values; a renderer fails clearly if they exceed its context limits. Bias is expressed
 * in normalized shadow depth, while normal bias is expressed in scene units.
 */
public abstract sealed class LightShadow permits DirectionalLightShadow, PointLightShadow, SpotLightShadow {
    /** Default width and height of a shadow map. */
    public static final int DEFAULT_MAP_SIZE = 512;

    private final boolean squareMapRequired;

    private int mapWidth;
    private int mapHeight;
    private float bias;
    private float normalBias;
    private float cameraNear;
    private float cameraFar;

    /** Creates default shadow configuration, optionally requiring square maps. */
    LightShadow(boolean squareMapRequired) {
        this.squareMapRequired = squareMapRequired;
        mapWidth = DEFAULT_MAP_SIZE;
        mapHeight = DEFAULT_MAP_SIZE;
        cameraNear = 0.5f;
        cameraFar = 500.0f;
    }

    /**
     * Returns the requested shadow-map width.
     *
     * @return positive pixel width, initially {@value #DEFAULT_MAP_SIZE}
     */
    public final int mapWidth() {
        return mapWidth;
    }

    /**
     * Returns the requested shadow-map height.
     *
     * @return positive pixel height, initially {@value #DEFAULT_MAP_SIZE}
     */
    public final int mapHeight() {
        return mapHeight;
    }

    /**
     * Changes the requested shadow-map dimensions.
     *
     * <p>Point-light cube-map faces must be square. Changing dimensions causes each renderer to
     * recreate that light's context-local shadow map on its next rendered frame.
     *
     * @param width positive pixel width
     * @param height positive pixel height
     * @throws IllegalArgumentException if a dimension is not positive or a required cube-map face
     *     is not square
     */
    public final void setMapSize(int width, int height) {
        int validWidth = Preconditions.requirePositive(width, "width");
        int validHeight = Preconditions.requirePositive(height, "height");
        if (squareMapRequired && validWidth != validHeight) {
            throw new IllegalArgumentException(
                    "Point-light shadow-map width and height must match: " + validWidth + " != " + validHeight);
        }
        mapWidth = validWidth;
        mapHeight = validHeight;
    }

    /**
     * Returns the normalized depth bias subtracted during comparison.
     *
     * @return finite depth bias, initially zero
     */
    public final float bias() {
        return bias;
    }

    /**
     * Changes the normalized depth bias subtracted during comparison.
     *
     * @param bias finite normalized depth offset
     * @throws IllegalArgumentException if {@code bias} is not finite
     */
    public final void setBias(float bias) {
        this.bias = Preconditions.requireFinite(bias, "bias");
    }

    /**
     * Returns the receiver offset along its surface normal.
     *
     * @return finite scene-unit offset, initially zero
     */
    public final float normalBias() {
        return normalBias;
    }

    /**
     * Changes the receiver offset along its surface normal.
     *
     * @param normalBias finite scene-unit offset
     * @throws IllegalArgumentException if {@code normalBias} is not finite
     */
    public final void setNormalBias(float normalBias) {
        this.normalBias = Preconditions.requireFinite(normalBias, "normalBias");
    }

    /**
     * Returns the shadow camera's near clipping distance.
     *
     * @return finite positive near distance, initially {@code 0.5}
     */
    public final float cameraNear() {
        return cameraNear;
    }

    /**
     * Returns the shadow camera's far clipping distance.
     *
     * @return finite far distance greater than {@link #cameraNear()}, initially {@code 500}
     */
    public final float cameraFar() {
        return cameraFar;
    }

    /**
     * Changes the shadow camera's clipping range atomically.
     *
     * @param near finite positive near distance
     * @param far finite distance greater than {@code near}
     * @throws IllegalArgumentException if the range is invalid
     */
    public final void setCameraRange(float near, float far) {
        float validNear = Preconditions.requirePositive(near, "near");
        Preconditions.requireLessThan(validNear, "near", far, "far");
        cameraNear = validNear;
        cameraFar = far;
    }
}
