/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import io.github.glynch.jscene3d.doom.geometry.DoomUnits;
import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.doom.runtime.DoomDoor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Descriptor-ready behavior derived from supported classic Doom manual-door linedefs. */
record DoomDoorPublication(
        int sectorIndex,
        DoomDoor.Profile profile,
        float closedHeight,
        float openHeight,
        float speed,
        float holdOpenSeconds) {
    private static final int MANUAL_OPEN_STAY = 31;
    private static final int MANUAL_BLAZE_RAISE = 117;
    private static final float OPEN_CLEARANCE = DoomUnits.toWorld(4.0F);
    private static final float CLASSIC_TICKS_PER_SECOND = 35.0F;
    private static final float NORMAL_SPEED = DoomUnits.toWorld(2.0F) * CLASSIC_TICKS_PER_SECOND;
    private static final float BLAZE_SPEED = DoomUnits.toWorld(8.0F) * CLASSIC_TICKS_PER_SECOND;
    private static final float BLAZE_HOLD_SECONDS = 150.0F / CLASSIC_TICKS_PER_SECOND;

    /** Discovers supported doors in deterministic source-sector order. */
    static List<DoomDoorPublication> discover(DoomMap map) {
        DoomMap source = Objects.requireNonNull(map, "map");
        Map<Integer, DoomDoorPublication> doors = new LinkedHashMap<>();
        for (DoomMap.Linedef linedef : source.linedefs()) {
            if (!isSupported(linedef.special()) || linedef.leftSidedef() < 0) {
                continue;
            }
            int sector = sectorForSide(source, linedef.leftSidedef());
            doors.computeIfAbsent(sector, ignored -> create(source, sector, linedef.special()));
        }
        return List.copyOf(doors.values());
    }

    /** Calculates one door's destination and timing from source map semantics. */
    private static DoomDoorPublication create(DoomMap map, int sectorIndex, int special) {
        float closed = DoomUnits.toWorld(map.sectors().get(sectorIndex).ceilingHeight());
        float adjacent = lowestAdjacentCeiling(map, sectorIndex);
        if (!Float.isFinite(adjacent) || adjacent - OPEN_CLEARANCE < closed) {
            throw new IllegalArgumentException("manual door sector has no valid open destination: " + sectorIndex);
        }
        boolean blaze = special == MANUAL_BLAZE_RAISE;
        return new DoomDoorPublication(
                sectorIndex,
                blaze ? DoomDoor.Profile.BLAZE : DoomDoor.Profile.NORMAL,
                closed,
                adjacent - OPEN_CLEARANCE,
                blaze ? BLAZE_SPEED : NORMAL_SPEED,
                blaze ? BLAZE_HOLD_SECONDS : 0.0F);
    }

    /** Finds the lowest ceiling on a sector sharing a two-sided boundary with the door. */
    private static float lowestAdjacentCeiling(DoomMap map, int sectorIndex) {
        float adjacent = Float.POSITIVE_INFINITY;
        for (DoomMap.Linedef candidate : map.linedefs()) {
            if (candidate.leftSidedef() < 0) {
                continue;
            }
            int right = sectorForSide(map, candidate.rightSidedef());
            int left = sectorForSide(map, candidate.leftSidedef());
            if (right == sectorIndex && left != sectorIndex) {
                adjacent = Math.min(adjacent, ceilingHeight(map, left));
            } else if (left == sectorIndex && right != sectorIndex) {
                adjacent = Math.min(adjacent, ceilingHeight(map, right));
            }
        }
        return adjacent;
    }

    private static float ceilingHeight(DoomMap map, int sectorIndex) {
        return DoomUnits.toWorld(map.sectors().get(sectorIndex).ceilingHeight());
    }

    private static int sectorForSide(DoomMap map, int sidedefIndex) {
        return map.sidedefs().get(sidedefIndex).sector();
    }

    private static boolean isSupported(int special) {
        return special == MANUAL_OPEN_STAY || special == MANUAL_BLAZE_RAISE;
    }
}
