/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

import java.util.List;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable outcome of one explicit kinematic-body move. */
public final class KinematicMoveResult {
    private final Vector3f appliedTranslation;
    private final Vector3f remainingTranslation;
    private final Vector3f groundNormal;
    private final boolean grounded;
    private final boolean stepped;
    private final List<KinematicContact> contacts;
    private final List<OverlapEvent> overlapEvents;

    /**
     * Creates an immutable result and copies its vectors and lists.
     *
     * @param appliedTranslation translation applied to the moving body
     * @param remainingTranslation requested translation left unresolved
     * @param groundNormal final walkable ground normal, or zero
     * @param grounded whether walkable ground was detected
     * @param stepped whether a bounded step was traversed
     * @param contacts solid contacts encountered while resolving movement
     * @param overlapEvents collision-sensor transitions at the final pose
     */
    public KinematicMoveResult(
            Vector3fc appliedTranslation,
            Vector3fc remainingTranslation,
            Vector3fc groundNormal,
            boolean grounded,
            boolean stepped,
            List<KinematicContact> contacts,
            List<OverlapEvent> overlapEvents) {
        this.appliedTranslation = new Vector3f(appliedTranslation);
        this.remainingTranslation = new Vector3f(remainingTranslation);
        this.groundNormal = new Vector3f(groundNormal);
        this.grounded = grounded;
        this.stepped = stepped;
        this.contacts = List.copyOf(contacts);
        this.overlapEvents = List.copyOf(overlapEvents);
    }

    /**
     * Copies the translation actually applied to the body.
     *
     * @param destination vector to receive the translation
     * @return the supplied destination
     */
    public Vector3f appliedTranslation(Vector3f destination) {
        return destination.set(appliedTranslation);
    }

    /**
     * Copies desired translation that could not be resolved within the configured limits.
     *
     * @param destination vector to receive the translation
     * @return the supplied destination
     */
    public Vector3f remainingTranslation(Vector3f destination) {
        return destination.set(remainingTranslation);
    }

    /**
     * Returns whether the final pose has walkable ground within snap distance.
     *
     * @return whether the body is grounded
     */
    public boolean isGrounded() {
        return grounded;
    }

    /**
     * Copies the final walkable ground normal, or zero when not grounded.
     *
     * @param destination vector to receive the normal
     * @return the supplied destination
     */
    public Vector3f groundNormal(Vector3f destination) {
        return destination.set(groundNormal);
    }

    /**
     * Returns whether this move traversed a bounded step.
     *
     * @return whether a step was traversed
     */
    public boolean stepped() {
        return stepped;
    }

    /**
     * Returns solid contacts in deterministic encounter order.
     *
     * @return immutable contact list
     */
    public List<KinematicContact> contacts() {
        return contacts;
    }

    /**
     * Returns collision-sensor transitions ordered by sensor identifier.
     *
     * @return immutable overlap event list
     */
    public List<OverlapEvent> overlapEvents() {
        return overlapEvents;
    }

    /**
     * Returns a copy with replacement collision-sensor transitions.
     *
     * @param newOverlapEvents replacement overlap events
     * @return updated immutable result
     */
    public KinematicMoveResult withOverlapEvents(List<OverlapEvent> newOverlapEvents) {
        return new KinematicMoveResult(
                appliedTranslation, remainingTranslation, groundNormal, grounded, stepped, contacts, newOverlapEvents);
    }
}
