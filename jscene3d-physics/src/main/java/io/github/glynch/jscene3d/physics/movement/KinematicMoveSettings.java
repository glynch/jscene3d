/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

import io.github.glynch.jscene3d.physics.internal.Preconditions;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable collision-resolution settings for one explicit kinematic move. */
public final class KinematicMoveSettings {
    /** General-purpose Y-up movement settings with sliding, steps, and ground snapping enabled. */
    public static final KinematicMoveSettings DEFAULT = new KinematicMoveSettings(
            new Vector3f(0.0F, 1.0F, 0.0F), 1.0E-3F, 4, 0.5F, 0.1F, (float) Math.toRadians(50.0));

    private final Vector3f up;
    private final float skinWidth;
    private final int maximumSlideIterations;
    private final float maximumStepHeight;
    private final float groundSnapDistance;
    private final float maximumSlopeAngle;

    private KinematicMoveSettings(
            Vector3fc up,
            float skinWidth,
            int maximumSlideIterations,
            float maximumStepHeight,
            float groundSnapDistance,
            float maximumSlopeAngle) {
        this.up = Preconditions.requireDirection(up, "up");
        Preconditions.requireNonNegative(skinWidth, "skinWidth");
        if (maximumSlideIterations < 1) {
            throw new IllegalArgumentException("maximumSlideIterations must be positive");
        }
        Preconditions.requireNonNegative(maximumStepHeight, "maximumStepHeight");
        Preconditions.requireNonNegative(groundSnapDistance, "groundSnapDistance");
        if (!Float.isFinite(maximumSlopeAngle) || maximumSlopeAngle < 0.0F || maximumSlopeAngle >= Math.PI / 2.0) {
            throw new IllegalArgumentException("maximumSlopeAngle must be finite and in [0, PI / 2)");
        }
        this.skinWidth = skinWidth;
        this.maximumSlideIterations = maximumSlideIterations;
        this.maximumStepHeight = maximumStepHeight;
        this.groundSnapDistance = groundSnapDistance;
        this.maximumSlopeAngle = maximumSlopeAngle;
    }

    /**
     * Copies the normalized world-space up direction into {@code destination}.
     *
     * @param destination vector to receive the direction
     * @return the supplied destination
     */
    public Vector3f up(Vector3f destination) {
        return destination.set(up);
    }

    /** Returns the gap retained between the moving shape and solid surfaces.
     * @return non-negative world-space gap
     */
    public float skinWidth() {
        return skinWidth;
    }

    /** Returns the maximum number of collision-and-slide iterations.
     * @return positive iteration limit
     */
    public int maximumSlideIterations() {
        return maximumSlideIterations;
    }

    /** Returns the largest obstacle height eligible for step traversal.
     * @return non-negative step height
     */
    public float maximumStepHeight() {
        return maximumStepHeight;
    }

    /** Returns the downward distance used to find and retain walkable ground.
     * @return non-negative snap distance
     */
    public float groundSnapDistance() {
        return groundSnapDistance;
    }

    /** Returns the steepest walkable slope angle in radians.
     * @return slope angle in {@code [0, PI / 2)}
     */
    public float maximumSlopeAngle() {
        return maximumSlopeAngle;
    }

    /** Returns a copy using a normalized version of the supplied up direction.
     * @param newUp finite non-zero up direction
     * @return updated immutable settings
     */
    public KinematicMoveSettings withUp(Vector3fc newUp) {
        return copy(newUp, skinWidth, maximumSlideIterations, maximumStepHeight, groundSnapDistance, maximumSlopeAngle);
    }

    /** Returns a copy using the supplied non-negative skin width.
     * @param newSkinWidth replacement skin width
     * @return updated immutable settings
     */
    public KinematicMoveSettings withSkinWidth(float newSkinWidth) {
        return copy(up, newSkinWidth, maximumSlideIterations, maximumStepHeight, groundSnapDistance, maximumSlopeAngle);
    }

    /** Returns a copy using the supplied positive slide-iteration limit.
     * @param newMaximumSlideIterations replacement iteration limit
     * @return updated immutable settings
     */
    public KinematicMoveSettings withMaximumSlideIterations(int newMaximumSlideIterations) {
        return copy(up, skinWidth, newMaximumSlideIterations, maximumStepHeight, groundSnapDistance, maximumSlopeAngle);
    }

    /** Returns a copy using the supplied non-negative maximum step height.
     * @param newMaximumStepHeight replacement maximum step height
     * @return updated immutable settings
     */
    public KinematicMoveSettings withMaximumStepHeight(float newMaximumStepHeight) {
        return copy(up, skinWidth, maximumSlideIterations, newMaximumStepHeight, groundSnapDistance, maximumSlopeAngle);
    }

    /** Returns a copy using the supplied non-negative ground-snap distance.
     * @param newGroundSnapDistance replacement ground-snap distance
     * @return updated immutable settings
     */
    public KinematicMoveSettings withGroundSnapDistance(float newGroundSnapDistance) {
        return copy(up, skinWidth, maximumSlideIterations, maximumStepHeight, newGroundSnapDistance, maximumSlopeAngle);
    }

    /** Returns a copy using the supplied maximum walkable slope angle in radians.
     * @param newMaximumSlopeAngle replacement slope angle
     * @return updated immutable settings
     */
    public KinematicMoveSettings withMaximumSlopeAngle(float newMaximumSlopeAngle) {
        return copy(up, skinWidth, maximumSlideIterations, maximumStepHeight, groundSnapDistance, newMaximumSlopeAngle);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof KinematicMoveSettings settings
                && up.equals(settings.up)
                && Float.compare(skinWidth, settings.skinWidth) == 0
                && maximumSlideIterations == settings.maximumSlideIterations
                && Float.compare(maximumStepHeight, settings.maximumStepHeight) == 0
                && Float.compare(groundSnapDistance, settings.groundSnapDistance) == 0
                && Float.compare(maximumSlopeAngle, settings.maximumSlopeAngle) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                up, skinWidth, maximumSlideIterations, maximumStepHeight, groundSnapDistance, maximumSlopeAngle);
    }

    @Override
    public String toString() {
        return "KinematicMoveSettings[up=" + up + ", skinWidth=" + skinWidth + ", maximumSlideIterations="
                + maximumSlideIterations + ", maximumStepHeight=" + maximumStepHeight + ", groundSnapDistance="
                + groundSnapDistance + ", maximumSlopeAngle=" + maximumSlopeAngle + ']';
    }

    private static KinematicMoveSettings copy(
            Vector3fc up,
            float skinWidth,
            int maximumSlideIterations,
            float maximumStepHeight,
            float groundSnapDistance,
            float maximumSlopeAngle) {
        return new KinematicMoveSettings(
                up, skinWidth, maximumSlideIterations, maximumStepHeight, groundSnapDistance, maximumSlopeAngle);
    }
}
