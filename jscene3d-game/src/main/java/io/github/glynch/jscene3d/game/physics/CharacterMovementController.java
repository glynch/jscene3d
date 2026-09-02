/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.physics;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.internal.Preconditions;
import io.github.glynch.jscene3d.physics.CharacterController;
import io.github.glynch.jscene3d.physics.movement.CharacterMoveResult;
import java.time.Duration;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Converts semantic movement actions into camera-relative planar character motion.
 *
 * <p>The supplied view direction defines forward independently of a particular camera implementation. Its component
 * along the physics controller's up axis is ignored, so the same controller supports first-person, third-person, and
 * non-camera callers. Diagonal input is normalized and a pressed jump action is forwarded exactly once to the
 * renderer-independent physics controller.
 */
public final class CharacterMovementController {
    private static final float MIN_DIRECTION_LENGTH_SQUARED = 1.0E-8F;

    private final CharacterController physicsController;
    private final CharacterMovementActions actions;
    private final float moveSpeed;
    private final Vector3f up;
    private final Vector3f planarForward = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f desiredVelocity = new Vector3f();
    private final Vector3f facingDirection = new Vector3f();

    /**
     * Creates a semantic movement controller.
     *
     * @param physicsController renderer-independent character physics
     * @param actions semantic actions used for locomotion
     * @param moveSpeed positive movement speed in world units per second
     */
    public CharacterMovementController(
            CharacterController physicsController, CharacterMovementActions actions, float moveSpeed) {
        this.physicsController = Objects.requireNonNull(physicsController, "physicsController");
        this.actions = Objects.requireNonNull(actions, "actions");
        this.moveSpeed = Preconditions.requirePositive(moveSpeed, "moveSpeed");
        up = physicsController.settings().movementSettings().up(new Vector3f());
        initializeFacingDirection();
    }

    /**
     * Advances semantic character movement by one fixed update.
     *
     * @param input semantic input snapshot for the update
     * @param viewForward finite world-space viewing direction
     * @param fixedStep positive fixed-update duration
     * @return immutable physics movement outcome
     */
    public CharacterMoveResult move(ActionSnapshot input, Vector3fc viewForward, Duration fixedStep) {
        ActionSnapshot validInput = Objects.requireNonNull(input, "input");
        Duration validFixedStep = Preconditions.requirePositive(fixedStep, "fixedStep");
        updateMovementBasis(Preconditions.requireFinite(viewForward, "viewForward"));
        if (validInput.wasPressed(actions.jump())) {
            physicsController.tryJump();
        }
        updateDesiredVelocity(validInput);
        float fixedSeconds = validFixedStep.toNanos() / 1_000_000_000.0F;
        return physicsController.move(desiredVelocity, fixedSeconds);
    }

    /**
     * Copies the most recently requested planar velocity.
     *
     * @param destination vector to receive the velocity
     * @return supplied destination
     */
    public Vector3f desiredVelocity(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(desiredVelocity);
    }

    /**
     * Copies the most recent non-zero movement direction.
     *
     * @param destination vector to receive the unit direction
     * @return supplied destination
     */
    public Vector3f facingDirection(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(facingDirection);
    }

    /** Returns the configured movement speed.
     * @return positive world units per second
     */
    public float moveSpeed() {
        return moveSpeed;
    }

    /** Returns the semantic action set.
     * @return immutable movement actions
     */
    public CharacterMovementActions actions() {
        return actions;
    }

    /** Chooses a deterministic initial direction perpendicular to the configured up axis. */
    private void initializeFacingDirection() {
        Vector3f reference = Math.abs(up.z) < 0.9F ? new Vector3f(0.0F, 0.0F, -1.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        facingDirection
                .set(reference)
                .sub(new Vector3f(up).mul(reference.dot(up)))
                .normalize();
    }

    /** Projects view forward onto the movement plane, retaining the last direction at a vertical singularity. */
    private void updateMovementBasis(Vector3fc viewForward) {
        planarForward.set(viewForward).sub(new Vector3f(up).mul(viewForward.dot(up)));
        if (planarForward.lengthSquared() <= MIN_DIRECTION_LENGTH_SQUARED) {
            planarForward.set(facingDirection);
        } else {
            planarForward.normalize();
        }
        planarForward.cross(up, right).normalize();
    }

    /** Resolves digital axes into one normalized planar velocity. */
    private void updateDesiredVelocity(ActionSnapshot input) {
        float strafe = input.axis(actions.left(), actions.right());
        float forward = input.axis(actions.backward(), actions.forward());
        desiredVelocity.set(planarForward).mul(forward).add(new Vector3f(right).mul(strafe));
        if (desiredVelocity.lengthSquared() > 1.0F) {
            desiredVelocity.normalize();
        }
        desiredVelocity.mul(moveSpeed);
        if (desiredVelocity.lengthSquared() > MIN_DIRECTION_LENGTH_SQUARED) {
            facingDirection.set(desiredVelocity).normalize();
        }
    }
}
