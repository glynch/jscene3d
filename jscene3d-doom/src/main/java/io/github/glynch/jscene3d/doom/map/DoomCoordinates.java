/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.map;

/** Converts classic Doom map coordinates to the JScene3D world-coordinate convention. */
public final class DoomCoordinates {
    private static final float MAP_UNITS_PER_WORLD_UNIT = 32.0F;

    /** Prevents construction of this stateless coordinate policy. */
    private DoomCoordinates() {
        throw new AssertionError("DoomCoordinates cannot be instantiated");
    }

    /**
     * Converts a Doom map measurement to JScene3D world units.
     *
     * @param mapUnits source map measurement
     * @return measurement in world units
     */
    public static float toWorld(float mapUnits) {
        return mapUnits / MAP_UNITS_PER_WORLD_UNIT;
    }

    /**
     * Converts the difference between two integral Doom coordinates to world units.
     *
     * @param end ending Doom coordinate
     * @param start starting Doom coordinate
     * @return signed difference in world units
     */
    public static float deltaToWorld(int end, int start) {
        return ((float) end - start) / MAP_UNITS_PER_WORLD_UNIT;
    }

    /**
     * Maps a Doom map y-coordinate onto the right-handed JScene3D z-axis.
     *
     * @param mapY source map y-coordinate
     * @return world z-coordinate
     */
    public static float yToWorldZ(double mapY) {
        return mapY == 0.0 ? 0.0F : toWorld((float) -mapY);
    }

    /**
     * Converts a JScene3D world measurement to Doom map units.
     *
     * @param worldUnits world measurement
     * @return source map measurement
     */
    public static double fromWorld(float worldUnits) {
        return worldUnits * MAP_UNITS_PER_WORLD_UNIT;
    }

    /**
     * Converts a JScene3D world measurement to Doom map units without widening it.
     *
     * @param worldUnits world measurement
     * @return source map measurement
     */
    public static float fromWorldFloat(float worldUnits) {
        return worldUnits * MAP_UNITS_PER_WORLD_UNIT;
    }

    /**
     * Maps a JScene3D world z-coordinate onto the Doom map y-axis.
     *
     * @param worldZ world z-coordinate
     * @return source map y-coordinate
     */
    public static double worldZToY(float worldZ) {
        return -fromWorld(worldZ);
    }
}
