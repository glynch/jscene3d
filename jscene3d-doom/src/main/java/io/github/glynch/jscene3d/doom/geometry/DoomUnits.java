/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.geometry;

/** Converts between authored Doom map units and the application's world coordinate system. */
public final class DoomUnits {
    private static final float UNITS_PER_WORLD_UNIT = 32.0F;

    /** Prevents construction of this stateless coordinate policy. */
    private DoomUnits() {
        throw new AssertionError("DoomUnits cannot be instantiated");
    }

    /**
     * Returns a Doom measurement in application world units.
     *
     * @param doomUnits source Doom measurement
     * @return application world measurement
     */
    public static float toWorld(float doomUnits) {
        return doomUnits / UNITS_PER_WORLD_UNIT;
    }

    /**
     * Returns the difference between two integral Doom coordinates in world units.
     *
     * @param end ending Doom coordinate
     * @param start starting Doom coordinate
     * @return signed application-world difference
     */
    public static float deltaToWorld(int end, int start) {
        return ((float) end - start) / UNITS_PER_WORLD_UNIT;
    }

    /**
     * Maps a Doom map y-coordinate onto the application's right-handed world z-axis.
     *
     * @param doomY source Doom y-coordinate
     * @return application world z-coordinate
     */
    public static float yToWorldZ(double doomY) {
        return doomY == 0.0 ? 0.0F : toWorld((float) -doomY);
    }

    /**
     * Returns an application world coordinate in Doom map units.
     *
     * @param worldUnits application world measurement
     * @return source Doom measurement
     */
    public static double fromWorld(float worldUnits) {
        return worldUnits * UNITS_PER_WORLD_UNIT;
    }

    /**
     * Returns an application measurement in source Doom map units without widening it.
     *
     * @param worldUnits application world measurement
     * @return source Doom measurement
     */
    public static float fromWorldFloat(float worldUnits) {
        return worldUnits * UNITS_PER_WORLD_UNIT;
    }

    /**
     * Maps an application world z-coordinate onto the Doom map y-axis.
     *
     * @param worldZ application world z-coordinate
     * @return source Doom y-coordinate
     */
    public static double worldZToY(float worldZ) {
        return -fromWorld(worldZ);
    }
}
