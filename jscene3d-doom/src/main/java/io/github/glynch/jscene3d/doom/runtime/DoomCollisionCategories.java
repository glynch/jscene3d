/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime;

/** Collision categories shared by imported Doom map mechanisms and game-authored actors. */
public final class DoomCollisionCategories {
    /** Player-controlled character bodies which may activate player-only map triggers. */
    public static final int PLAYER = 1 << 1;

    /** Sensors which detect characters obstructing moving doors. */
    public static final int DOOR_OBSTRUCTION_SENSOR = 1 << 2;

    /** Non-player combatants which obstruct doors but do not activate player-only map triggers. */
    public static final int MONSTER = 1 << 3;

    /** Walk-over sensors authored from player-activated Doom linedefs. */
    public static final int PLAYER_TRIGGER_SENSOR = 1 << 4;

    /** Every character category which must obstruct a moving door. */
    public static final int DOOR_OBSTRUCTIONS = PLAYER | MONSTER;

    private DoomCollisionCategories() {
        throw new AssertionError("DoomCollisionCategories cannot be instantiated");
    }
}
