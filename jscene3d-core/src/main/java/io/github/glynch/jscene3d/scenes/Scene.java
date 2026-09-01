/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.scenes;

import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.textures.EnvironmentMap;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

/** Root scene node with renderer-independent scene settings. */
public final class Scene extends Object3D {
    private final Quaternionf environmentRotation = new Quaternionf();

    private @Nullable Color background;
    private @Nullable EnvironmentMap backgroundEnvironment;
    private @Nullable EnvironmentMap environment;
    private float backgroundIntensity = 1.0f;
    private float environmentIntensity = 1.0f;

    /** Creates an empty scene with no background override. */
    public Scene() {
        super();
    }

    /**
     * Returns the optional solid background color.
     *
     * @return the background, or {@code null} when the renderer default applies
     */
    public @Nullable Color background() {
        return background;
    }

    /**
     * Sets the solid background color.
     *
     * @param background background color
     * @throws NullPointerException if {@code background} is {@code null}
     */
    public void setBackground(Color background) {
        this.background = Objects.requireNonNull(background, "background");
        backgroundEnvironment = null;
    }

    /**
     * Returns the optional environment map drawn behind scene objects.
     *
     * @return the shared background environment, or {@code null} when none is selected
     */
    public @Nullable EnvironmentMap backgroundEnvironment() {
        return backgroundEnvironment;
    }

    /**
     * Selects an open environment map as the visible scene background.
     *
     * <p>This setting is independent of image-based lighting. Selecting an environment background
     * clears the solid background color without transferring ownership.
     *
     * @param background shared open environment map
     * @throws NullPointerException if {@code background} is {@code null}
     * @throws IllegalArgumentException if {@code background} is closed
     */
    public void setBackgroundEnvironment(EnvironmentMap background) {
        backgroundEnvironment = requireOpen(background, "background");
        this.background = null;
    }

    /**
     * Returns the visible environment-background multiplier.
     *
     * @return finite non-negative intensity, initially one
     */
    public float backgroundIntensity() {
        return backgroundIntensity;
    }

    /**
     * Changes the visible environment-background multiplier without changing lighting.
     *
     * @param backgroundIntensity finite non-negative multiplier
     * @throws IllegalArgumentException if the value is negative or non-finite
     */
    public void setBackgroundIntensity(float backgroundIntensity) {
        this.backgroundIntensity = Preconditions.requireNonNegative(backgroundIntensity, "backgroundIntensity");
    }

    /** Clears the background so the renderer's default clear color applies. */
    public void clearBackground() {
        background = null;
        backgroundEnvironment = null;
    }

    /**
     * Returns the optional environment used for image-based lighting.
     *
     * @return shared lighting environment, or {@code null} when image-based lighting is disabled
     */
    public @Nullable EnvironmentMap environment() {
        return environment;
    }

    /**
     * Selects an open environment map for image-based lighting.
     *
     * @param environment shared open environment map
     * @throws NullPointerException if {@code environment} is {@code null}
     * @throws IllegalArgumentException if {@code environment} is closed
     */
    public void setEnvironment(EnvironmentMap environment) {
        this.environment = requireOpen(environment, "environment");
    }

    /** Disables image-based lighting without closing the previously shared environment map. */
    public void clearEnvironment() {
        environment = null;
    }

    /**
     * Returns the scene-wide image-based-lighting multiplier.
     *
     * @return finite non-negative intensity, initially one
     */
    public float environmentIntensity() {
        return environmentIntensity;
    }

    /**
     * Changes the scene-wide image-based-lighting multiplier.
     *
     * @param environmentIntensity finite non-negative multiplier
     * @throws IllegalArgumentException if the value is negative or non-finite
     */
    public void setEnvironmentIntensity(float environmentIntensity) {
        this.environmentIntensity = Preconditions.requireNonNegative(environmentIntensity, "environmentIntensity");
    }

    /**
     * Copies the environment orientation into a destination quaternion.
     *
     * @param destination quaternion receiving the current orientation
     * @return {@code destination}
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public Quaternionf environmentRotation(Quaternionf destination) {
        return Objects.requireNonNull(destination, "destination").set(environmentRotation);
    }

    /**
     * Changes the environment orientation from intrinsic XYZ rotations in radians.
     *
     * @param xRadians finite rotation about X
     * @param yRadians finite rotation about Y
     * @param zRadians finite rotation about Z
     * @throws IllegalArgumentException if a value is non-finite
     */
    public void setEnvironmentRotation(float xRadians, float yRadians, float zRadians) {
        environmentRotation.rotationXYZ(
                Preconditions.requireFinite(xRadians, "xRadians"),
                Preconditions.requireFinite(yRadians, "yRadians"),
                Preconditions.requireFinite(zRadians, "zRadians"));
    }

    /**
     * Copies and normalizes an existing finite non-zero quaternion into the environment
     * orientation.
     *
     * @param rotation finite non-zero orientation
     * @throws NullPointerException if {@code rotation} is {@code null}
     * @throws IllegalArgumentException if {@code rotation} is invalid
     */
    public void setEnvironmentRotation(Quaternionfc rotation) {
        Quaternionfc validRotation = Preconditions.requireFinite(rotation, "rotation");
        if (validRotation.lengthSquared() == 0.0f) {
            throw new IllegalArgumentException("rotation must not be zero");
        }
        environmentRotation.set(validRotation).normalize();
    }

    /** Restores the identity environment orientation. */
    public void resetEnvironmentRotation() {
        environmentRotation.identity();
    }

    /** Requires a shared environment description to remain open. */
    private static EnvironmentMap requireOpen(EnvironmentMap environmentMap, String parameterName) {
        EnvironmentMap validEnvironmentMap = Objects.requireNonNull(environmentMap, parameterName);
        if (validEnvironmentMap.isClosed()) {
            throw new IllegalArgumentException(parameterName + " must be open");
        }
        return validEnvironmentMap;
    }
}
