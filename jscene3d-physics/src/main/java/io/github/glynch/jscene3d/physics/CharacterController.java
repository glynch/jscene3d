/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import io.github.glynch.jscene3d.physics.internal.Preconditions;
import io.github.glynch.jscene3d.physics.movement.CharacterControllerSettings;
import io.github.glynch.jscene3d.physics.movement.CharacterMoveResult;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveResult;
import java.util.Objects;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Applies gravity, jumping, grounding, sliding, and stepping to one kinematic body.
 *
 * <p>The caller supplies planar velocity once per fixed update. This class owns only movement state and has no
 * dependency on rendering, input devices, or a game loop.
 */
public final class CharacterController {
    private static final float CEILING_NORMAL_THRESHOLD = -1.0E-4F;

    private final PhysicsWorld world;
    private final KinematicBody body;
    private final CharacterControllerSettings settings;
    private final Vector3f up;
    private final Vector3f groundNormal = new Vector3f();
    private float verticalSpeed;
    private boolean grounded;
    private boolean jumpPending;

    /**
     * Creates a controller using the default settings.
     *
     * @param world world that owns the controlled body
     * @param body registered kinematic body to control
     */
    public CharacterController(PhysicsWorld world, KinematicBody body) {
        this(world, body, CharacterControllerSettings.DEFAULT);
    }

    /**
     * Creates a controller for one registered kinematic body.
     *
     * @param world world that owns the controlled body
     * @param body registered kinematic body to control
     * @param settings immutable movement settings
     */
    public CharacterController(PhysicsWorld world, KinematicBody body, CharacterControllerSettings settings) {
        this.world = Objects.requireNonNull(world, "world");
        this.body = Objects.requireNonNull(body, "body");
        this.settings = Objects.requireNonNull(settings, "settings");
        if (body.world() != world || !body.isRegistered()) {
            throw new IllegalArgumentException("body is not registered with this world");
        }
        up = settings.movementSettings().up(new Vector3f());
    }

    /** Returns the controlled body.
     * @return registered kinematic body
     */
    public KinematicBody body() {
        return body;
    }

    /** Returns the immutable controller settings.
     * @return controller settings
     */
    public CharacterControllerSettings settings() {
        return settings;
    }

    /** Returns whether the last update found walkable ground.
     * @return whether the character is grounded
     */
    public boolean isGrounded() {
        return grounded;
    }

    /** Copies the last walkable ground normal, or zero when airborne.
     * @param destination vector to receive the normal
     * @return supplied destination
     */
    public Vector3f groundNormal(Vector3f destination) {
        return destination.set(groundNormal);
    }

    /** Copies the controller's vertical velocity.
     * @param destination vector to receive velocity along the configured up axis
     * @return supplied destination
     */
    public Vector3f verticalVelocity(Vector3f destination) {
        return destination.set(up).mul(verticalSpeed);
    }

    /**
     * Requests a jump for the next fixed update when the character is grounded.
     *
     * @return {@code true} when the jump was accepted
     */
    public boolean tryJump() {
        if (!grounded || settings.jumpSpeed() == 0.0F) {
            return false;
        }
        verticalSpeed = settings.jumpSpeed();
        grounded = false;
        groundNormal.zero();
        jumpPending = true;
        return true;
    }

    /**
     * Advances movement by one fixed update.
     *
     * @param planarVelocity desired world-space velocity; its component along up is ignored
     * @param fixedSeconds positive fixed-update duration in seconds
     * @return immutable movement outcome
     */
    public CharacterMoveResult move(Vector3fc planarVelocity, float fixedSeconds) {
        Vector3f horizontalVelocity = Preconditions.requireFinite(planarVelocity, "planarVelocity");
        Preconditions.requirePositive(fixedSeconds, "fixedSeconds");
        horizontalVelocity.sub(new Vector3f(up).mul(horizontalVelocity.dot(up)));
        verticalSpeed -= settings.gravity() * fixedSeconds;
        Vector3f velocity = new Vector3f(horizontalVelocity).add(new Vector3f(up).mul(verticalSpeed));
        Vector3f requestedTranslation = new Vector3f(velocity).mul(fixedSeconds);
        KinematicMoveResult movement = world.move(body, requestedTranslation, settings.movementSettings());
        boolean jumped = jumpPending;
        jumpPending = false;
        updateVerticalState(movement);
        velocity.set(movement.appliedTranslation(new Vector3f())).div(fixedSeconds);
        return new CharacterMoveResult(requestedTranslation, velocity, jumped, movement);
    }

    /**
     * Repositions the body and clears controller velocity, grounding, and pending jump state.
     *
     * @param position new world-space body position
     * @param orientation new world-space body orientation; normalized internally
     */
    public void teleport(Vector3fc position, Quaternionfc orientation) {
        body.setTransform(position, orientation);
        verticalSpeed = 0.0F;
        grounded = false;
        groundNormal.zero();
        jumpPending = false;
    }

    private void updateVerticalState(KinematicMoveResult movement) {
        grounded = movement.isGrounded();
        movement.groundNormal(groundNormal);
        if ((grounded && verticalSpeed < 0.0F) || (verticalSpeed > 0.0F && hitCeiling(movement))) {
            verticalSpeed = 0.0F;
        }
    }

    private boolean hitCeiling(KinematicMoveResult movement) {
        return movement.contacts().stream()
                .map(contact -> contact.normal(new Vector3f()))
                .anyMatch(normal -> normal.dot(up) < CEILING_NORMAL_THRESHOLD);
    }
}
