/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import java.util.List;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable project-level outcome of one fixed character movement update. */
public final class CharacterMove3dResult {
    private final Vector3f requestedTranslation;
    private final Vector3f appliedTranslation;
    private final Vector3f velocity;
    private final Vector3f groundNormal;
    private final CharacterMove3dState state;
    private final List<CharacterContact3d> contacts;

    /**
     * Creates one immutable result by copying vectors and the contact list.
     *
     * @param requestedTranslation translation requested for this update
     * @param appliedTranslation translation actually applied to the body
     * @param velocity controller velocity after collision resolution
     * @param groundNormal final walkable-ground normal, or zero when airborne
     * @param state grounded, stepped, and jumped state for this update
     * @param contacts solid contacts in deterministic encounter order
     */
    public CharacterMove3dResult(
            Vector3fc requestedTranslation,
            Vector3fc appliedTranslation,
            Vector3fc velocity,
            Vector3fc groundNormal,
            CharacterMove3dState state,
            List<CharacterContact3d> contacts) {
        this.requestedTranslation = CollisionPreconditions.requireFinite(requestedTranslation, "requestedTranslation");
        this.appliedTranslation = CollisionPreconditions.requireFinite(appliedTranslation, "appliedTranslation");
        this.velocity = CollisionPreconditions.requireFinite(velocity, "velocity");
        this.groundNormal = CollisionPreconditions.requireFinite(groundNormal, "groundNormal");
        this.state = Objects.requireNonNull(state, "state");
        this.contacts = List.copyOf(Objects.requireNonNull(contacts, "contacts"));
    }

    /**
     * Copies the translation requested for this update.
     *
     * @param destination vector to receive the translation
     * @return supplied destination
     */
    public Vector3f requestedTranslation(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(requestedTranslation);
    }

    /**
     * Copies the translation actually applied to the body.
     *
     * @param destination vector to receive the translation
     * @return supplied destination
     */
    public Vector3f appliedTranslation(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(appliedTranslation);
    }

    /**
     * Copies controller velocity after collision resolution.
     *
     * @param destination vector to receive the velocity
     * @return supplied destination
     */
    public Vector3f velocity(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(velocity);
    }

    /**
     * Copies the final walkable-ground normal, or zero when airborne.
     *
     * @param destination vector to receive the normal
     * @return supplied destination
     */
    public Vector3f groundNormal(Vector3f destination) {
        return Objects.requireNonNull(destination, "destination").set(groundNormal);
    }

    /**
     * Returns whether the final pose has walkable ground.
     *
     * @return whether the final pose is grounded
     */
    public boolean isGrounded() {
        return state.grounded();
    }

    /**
     * Returns whether this update traversed a bounded step.
     *
     * @return whether the update stepped
     */
    public boolean stepped() {
        return state.stepped();
    }

    /**
     * Returns whether this update consumed a successful jump request.
     *
     * @return whether the update jumped
     */
    public boolean jumped() {
        return state.jumped();
    }

    /**
     * Returns solid contacts in deterministic encounter order.
     *
     * @return immutable contact list
     */
    public List<CharacterContact3d> contacts() {
        return contacts;
    }
}
