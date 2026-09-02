/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

import java.util.List;
import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable outcome of one fixed character-controller update. */
public final class CharacterMoveResult {
    private final Vector3f requestedTranslation;
    private final Vector3f appliedTranslation;
    private final Vector3f velocity;
    private final Vector3f groundNormal;
    private final boolean grounded;
    private final boolean stepped;
    private final boolean jumped;
    private final List<KinematicContact> contacts;
    private final List<OverlapEvent> overlapEvents;

    /**
     * Creates an immutable character movement result by copying vectors and lists.
     *
     * @param requestedTranslation translation requested for this update
     * @param velocity controller velocity after collision resolution
     * @param jumped whether this update consumed a successful jump request
     * @param movement resolved low-level kinematic movement
     */
    public CharacterMoveResult(
            Vector3fc requestedTranslation, Vector3fc velocity, boolean jumped, KinematicMoveResult movement) {
        Objects.requireNonNull(movement, "movement");
        this.requestedTranslation = new Vector3f(requestedTranslation);
        this.appliedTranslation = movement.appliedTranslation(new Vector3f());
        this.velocity = new Vector3f(velocity);
        this.groundNormal = movement.groundNormal(new Vector3f());
        this.grounded = movement.isGrounded();
        this.stepped = movement.stepped();
        this.jumped = jumped;
        this.contacts = List.copyOf(movement.contacts());
        this.overlapEvents = List.copyOf(movement.overlapEvents());
    }

    /** Copies the translation requested for this update.
     * @param destination vector to receive the translation
     * @return supplied destination
     */
    public Vector3f requestedTranslation(Vector3f destination) {
        return destination.set(requestedTranslation);
    }

    /** Copies the translation actually applied to the body.
     * @param destination vector to receive the translation
     * @return supplied destination
     */
    public Vector3f appliedTranslation(Vector3f destination) {
        return destination.set(appliedTranslation);
    }

    /** Copies controller velocity after collision resolution.
     * @param destination vector to receive the velocity
     * @return supplied destination
     */
    public Vector3f velocity(Vector3f destination) {
        return destination.set(velocity);
    }

    /** Copies the final walkable ground normal, or zero when airborne.
     * @param destination vector to receive the normal
     * @return supplied destination
     */
    public Vector3f groundNormal(Vector3f destination) {
        return destination.set(groundNormal);
    }

    /** Returns whether the final pose has walkable ground.
     * @return whether the character is grounded
     */
    public boolean isGrounded() {
        return grounded;
    }

    /** Returns whether this update traversed a bounded step.
     * @return whether a step was traversed
     */
    public boolean stepped() {
        return stepped;
    }

    /** Returns whether this update consumed a successful jump request.
     * @return whether the character jumped
     */
    public boolean jumped() {
        return jumped;
    }

    /** Returns solid contacts in deterministic encounter order.
     * @return immutable contact list
     */
    public List<KinematicContact> contacts() {
        return contacts;
    }

    /** Returns collision-sensor transitions ordered by sensor identifier.
     * @return immutable overlap event list
     */
    public List<OverlapEvent> overlapEvents() {
        return overlapEvents;
    }
}
