/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Parallel illumination directed from this scene node's world position toward a world-space target.
 *
 * <p>The target is a copied point rather than another scene object. Moving or parenting the light
 * changes its world position while leaving the target fixed in world space. A default light starts
 * at {@code (0, 1, 0)} and targets the origin.
 */
public final class DirectionalLight extends ShadowCastingLight {
    private final Vector3f target;
    private final DirectionalLightShadow shadow;

    /** Creates a white directional light with unit intensity targeting the origin. */
    public DirectionalLight() {
        super();
        target = new Vector3f();
        shadow = new DirectionalLightShadow();
        setPosition(0.0f, 1.0f, 0.0f);
    }

    /**
     * Creates a directional light with unit intensity targeting the origin.
     *
     * @param color immutable linear-sRGB light color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public DirectionalLight(Color color) {
        super(color);
        target = new Vector3f();
        shadow = new DirectionalLightShadow();
        setPosition(0.0f, 1.0f, 0.0f);
    }

    /**
     * Creates a directional light with the supplied intensity targeting the origin.
     *
     * @param color immutable linear-sRGB light color
     * @param intensity finite non-negative intensity multiplier
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code intensity} is negative or non-finite
     */
    public DirectionalLight(Color color, float intensity) {
        super(color, intensity);
        target = new Vector3f();
        shadow = new DirectionalLightShadow();
        setPosition(0.0f, 1.0f, 0.0f);
    }

    /**
     * Returns this light's stable orthographic shadow description.
     *
     * @return owned directional-light shadow configuration
     */
    @Override
    public DirectionalLightShadow shadow() {
        return shadow;
    }

    /**
     * Copies the world-space target point into caller-owned storage.
     *
     * @param destination vector receiving the target point
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public Vector3f target(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(target);
    }

    /**
     * Changes the copied world-space target point.
     *
     * @param x finite world-space X coordinate
     * @param y finite world-space Y coordinate
     * @param z finite world-space Z coordinate
     * @throws IllegalArgumentException if any coordinate is not finite
     */
    public void setTarget(float x, float y, float z) {
        float validX = Preconditions.requireFinite(x, "x");
        float validY = Preconditions.requireFinite(y, "y");
        float validZ = Preconditions.requireFinite(z, "z");
        target.set(validX, validY, validZ);
    }

    /**
     * Copies an existing value into the world-space target point.
     *
     * @param target target point to copy
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws IllegalArgumentException if any coordinate is not finite
     */
    public void setTarget(Vector3fc target) {
        Vector3fc validTarget = Preconditions.requireFinite(target, "target");
        setTarget(validTarget.x(), validTarget.y(), validTarget.z());
    }
}
