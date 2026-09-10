/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime;

/** Runtime capability of one descriptor-authored vertically moving Doom floor. */
public interface DoomFloor {
    /** Source behavior retained explicitly for diagnostics, presentation, and later editor authoring. */
    enum Profile {
        /** Walk once, then lower the tagged floor to its highest surrounding floor. */
        WALK_ONCE_LOWER_TO_HIGHEST_SURROUNDING
    }

    /** Observable phases of the one-way moving-floor behavior. */
    enum Phase {
        /** The floor remains at its source-map height and has not been activated. */
        RAISED,
        /** The floor is moving toward its source-derived lower destination. */
        LOWERING,
        /** The floor has reached its lower destination and remains there. */
        LOWERED
    }

    /**
     * Requests activation using the imported source behavior.
     *
     * @return whether this request began movement
     */
    boolean activate();

    /**
     * Returns the exact imported source behavior.
     *
     * @return the imported behavior profile
     */
    Profile profile();

    /**
     * Returns the current movement phase.
     *
     * @return the current phase
     */
    Phase phase();

    /**
     * Returns the current local height in project world units.
     *
     * @return the floor height in project world units
     */
    float currentHeight();
}
