/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import java.time.Duration;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Live descriptor-backed collision body moved explicitly by gameplay behavior. */
public interface CharacterBody3d extends CollisionObject3d {
    /**
     * Repositions the character immediately and clears accumulated movement state.
     *
     * <p>This operation is valid before world activation, allowing launch configuration and editor play-from-here
     * workflows to establish an initial pose through the physics-authoritative character module.
     *
     * @param position new finite world-space position
     * @param orientation new finite world-space orientation; normalized internally
     */
    void teleport(Vector3fc position, Quaternionfc orientation);

    /**
     * Resolves one fixed update of world-space planar velocity through physics.
     *
     * <p>The velocity component along the configured up direction is ignored. Gravity and accepted jump requests are
     * applied internally. The owning physics module publishes the resolved pose to the entity's authoritative
     * {@code Transform3d} during the physics phase.
     *
     * @param planarVelocity desired finite world-space planar velocity
     * @param fixedStep positive fixed-update duration
     * @return immutable project-level movement result
     */
    CharacterMove3dResult move(Vector3fc planarVelocity, Duration fixedStep);

    /**
     * Requests a jump when the character is currently grounded.
     *
     * @return whether the request was accepted
     */
    boolean tryJump();

    /**
     * Returns whether the most recent movement update found walkable ground.
     *
     * @return whether the character is grounded
     */
    boolean isGrounded();

    /**
     * Copies the most recent walkable-ground normal, or zero when airborne.
     *
     * @param destination vector to receive the normal
     * @return supplied destination
     */
    Vector3f groundNormal(Vector3f destination);
}
