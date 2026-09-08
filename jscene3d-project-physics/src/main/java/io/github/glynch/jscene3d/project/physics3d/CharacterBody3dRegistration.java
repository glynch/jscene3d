/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import java.time.Duration;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Backend registration providing character movement without exposing low-level physics handles. */
public interface CharacterBody3dRegistration extends CollisionObject3dRegistration {
    /**
     * Resolves one fixed character movement update.
     *
     * @param planarVelocity desired finite world-space planar velocity
     * @param fixedStep positive fixed-update duration
     * @return immutable project-level movement result
     */
    CharacterMove3dResult move(Vector3fc planarVelocity, Duration fixedStep);

    /**
     * Requests a jump when grounded.
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
