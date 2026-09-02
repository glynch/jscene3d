/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

import io.github.glynch.jscene3d.physics.internal.Preconditions;
import java.util.Objects;

/** Immutable settings for gravity, jumping, and kinematic character collision resolution. */
public final class CharacterControllerSettings {
    /** General-purpose character settings using the default kinematic movement configuration. */
    public static final CharacterControllerSettings DEFAULT =
            new CharacterControllerSettings(KinematicMoveSettings.DEFAULT, 18.0F, 7.0F);

    private final KinematicMoveSettings movementSettings;
    private final float gravity;
    private final float jumpSpeed;

    private CharacterControllerSettings(KinematicMoveSettings movementSettings, float gravity, float jumpSpeed) {
        this.movementSettings = Objects.requireNonNull(movementSettings, "movementSettings");
        this.gravity = Preconditions.requireNonNegative(gravity, "gravity");
        this.jumpSpeed = Preconditions.requireNonNegative(jumpSpeed, "jumpSpeed");
    }

    /** Returns the low-level collision-resolution settings.
     * @return immutable kinematic movement settings
     */
    public KinematicMoveSettings movementSettings() {
        return movementSettings;
    }

    /** Returns acceleration applied opposite the configured up direction.
     * @return non-negative gravity acceleration
     */
    public float gravity() {
        return gravity;
    }

    /** Returns the initial upward speed used by a successful jump.
     * @return non-negative jump speed
     */
    public float jumpSpeed() {
        return jumpSpeed;
    }

    /** Returns a copy with replacement collision-resolution settings.
     * @param newMovementSettings replacement movement settings
     * @return updated immutable settings
     */
    public CharacterControllerSettings withMovementSettings(KinematicMoveSettings newMovementSettings) {
        return new CharacterControllerSettings(newMovementSettings, gravity, jumpSpeed);
    }

    /** Returns a copy with replacement gravity acceleration.
     * @param newGravity non-negative gravity acceleration
     * @return updated immutable settings
     */
    public CharacterControllerSettings withGravity(float newGravity) {
        return new CharacterControllerSettings(movementSettings, newGravity, jumpSpeed);
    }

    /** Returns a copy with replacement jump speed.
     * @param newJumpSpeed non-negative initial upward speed
     * @return updated immutable settings
     */
    public CharacterControllerSettings withJumpSpeed(float newJumpSpeed) {
        return new CharacterControllerSettings(movementSettings, gravity, newJumpSpeed);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof CharacterControllerSettings settings
                && movementSettings.equals(settings.movementSettings)
                && Float.compare(gravity, settings.gravity) == 0
                && Float.compare(jumpSpeed, settings.jumpSpeed) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(movementSettings, gravity, jumpSpeed);
    }

    @Override
    public String toString() {
        return "CharacterControllerSettings[movementSettings=" + movementSettings + ", gravity=" + gravity
                + ", jumpSpeed=" + jumpSpeed + ']';
    }
}
