/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime;

/** Runtime capability of one descriptor-authored vertically opening Doom door. */
public interface DoomDoor {
    /** Motion phases exposed for diagnostics and tests. */
    enum Phase {
        /** The door is at its closed height. */
        CLOSED,
        /** The door is moving upward. */
        OPENING,
        /** The door is open and will remain open. */
        OPENED,
        /** The door is holding at its open height before closing. */
        WAITING,
        /** The door is moving downward. */
        CLOSING
    }

    /**
     * Requests activation using the behavior authored by the importer.
     *
     * @return whether the request changed the door's phase
     */
    boolean activate();

    /**
     * Returns the current motion phase.
     *
     * @return current phase
     */
    Phase phase();

    /**
     * Returns the current local height of the moving door entity.
     *
     * @return current height in project world units
     */
    float currentHeight();
}
