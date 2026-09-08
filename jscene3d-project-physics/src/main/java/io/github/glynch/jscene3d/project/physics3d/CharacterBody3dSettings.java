/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/**
 * Authored gameplay-scale settings for one explicitly moved character body.
 *
 * @param gravity non-negative downward acceleration
 * @param jumpSpeed non-negative upward speed applied to accepted jumps
 * @param maximumStepHeight non-negative maximum height traversed as a step
 * @param groundSnapDistance non-negative distance used to retain walkable ground
 */
public record CharacterBody3dSettings(
        float gravity, float jumpSpeed, float maximumStepHeight, float groundSnapDistance) {
    /** General-purpose character settings shared by descriptor defaults and direct module clients. */
    public static final CharacterBody3dSettings DEFAULT = new CharacterBody3dSettings(18.0F, 7.0F, 0.5F, 0.1F);

    /** Validates one immutable settings value. */
    public CharacterBody3dSettings {
        CollisionPreconditions.requireNonNegative(gravity, "gravity");
        CollisionPreconditions.requireNonNegative(jumpSpeed, "jumpSpeed");
        CollisionPreconditions.requireNonNegative(maximumStepHeight, "maximumStepHeight");
        CollisionPreconditions.requireNonNegative(groundSnapDistance, "groundSnapDistance");
    }
}
