/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_THREE;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Point-source illumination constrained to a cone directed toward a world-space target.
 *
 * <p>The target is copied rather than retained as another scene object. A default light starts at
 * {@code (0, 1, 0)}, targets the origin, has a 60-degree outer angle, a hard cone edge, unlimited
 * distance, and inverse-square decay. Projected texture maps are not supported.
 */
public final class SpotLight extends ShadowCastingLight {
    private final Vector3f target;
    private final SpotLightShadow shadow;

    private float distance;
    private float decay;
    private float angle;
    private float penumbra;

    /** Creates a default white spotlight with unit intensity. */
    public SpotLight() {
        this(Color.WHITE, 1.0f);
    }

    /**
     * Creates a default spotlight with unit intensity.
     *
     * @param color immutable linear-sRGB light color
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public SpotLight(Color color) {
        this(color, 1.0f);
    }

    /**
     * Creates a default spotlight with the supplied intensity.
     *
     * @param color immutable linear-sRGB light color
     * @param intensity finite non-negative intensity multiplier
     * @throws NullPointerException if {@code color} is {@code null}
     * @throws IllegalArgumentException if {@code intensity} is negative or non-finite
     */
    public SpotLight(Color color, float intensity) {
        super(color, intensity);
        target = new Vector3f();
        shadow = new SpotLightShadow();
        decay = 2.0f;
        angle = PI_OVER_THREE;
        setPosition(0.0f, 1.0f, 0.0f);
    }

    /**
     * Returns this light's stable perspective shadow description.
     *
     * @return owned spotlight shadow configuration
     */
    @Override
    public SpotLightShadow shadow() {
        return shadow;
    }

    /**
     * Returns the maximum influence distance.
     *
     * @return zero for unlimited influence, or a positive scene-unit distance
     */
    public float distance() {
        return distance;
    }

    /**
     * Changes the maximum influence distance.
     *
     * @param distance zero for unlimited influence, or a positive scene-unit distance
     * @throws IllegalArgumentException if {@code distance} is negative or non-finite
     */
    public void setDistance(float distance) {
        this.distance = Preconditions.requireNonNegative(distance, "distance");
    }

    /**
     * Returns the distance-attenuation exponent.
     *
     * @return finite non-negative exponent, initially two
     */
    public float decay() {
        return decay;
    }

    /**
     * Changes the distance-attenuation exponent.
     *
     * @param decay finite non-negative attenuation exponent
     * @throws IllegalArgumentException if {@code decay} is negative or non-finite
     */
    public void setDecay(float decay) {
        this.decay = Preconditions.requireNonNegative(decay, "decay");
    }

    /**
     * Returns the spotlight outer half-angle in radians.
     *
     * @return angle in {@code (0, PI / 2]}, initially {@code PI / 3}
     */
    public float angle() {
        return angle;
    }

    /**
     * Changes the spotlight outer half-angle.
     *
     * @param angle finite radians in {@code (0, PI / 2]}
     * @throws IllegalArgumentException if {@code angle} is outside its valid range
     */
    public void setAngle(float angle) {
        float validAngle = Preconditions.requirePositive(angle, "angle");
        if (validAngle > PI_OVER_TWO) {
            throw new IllegalArgumentException("angle must not exceed PI / 2 radians: " + validAngle);
        }
        this.angle = validAngle;
    }

    /**
     * Returns the fraction of the cone softened from its outer edge inward.
     *
     * @return value in {@code [0, 1]}, initially zero
     */
    public float penumbra() {
        return penumbra;
    }

    /**
     * Changes the fraction of the cone softened from its outer edge inward.
     *
     * @param penumbra finite value in {@code [0, 1]}
     * @throws IllegalArgumentException if {@code penumbra} is outside its valid range
     */
    public void setPenumbra(float penumbra) {
        this.penumbra = Preconditions.requireInRange(penumbra, 0.0f, 1.0f, "penumbra");
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
