/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import io.github.glynch.jscene3d.doom.geometry.DoomUnits;
import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.doom.runtime.DoomFloor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Descriptor-ready moving floors and walk-over triggers derived from supported linedef specials. */
record DoomFloorPublication(
        int sectorIndex,
        DoomFloor.Profile profile,
        float raisedHeight,
        float loweredHeight,
        float speed,
        List<Integer> triggerLinedefs) {
    private static final int WALK_ONCE_LOWER_TO_HIGHEST_SURROUNDING = 19;
    private static final float CLASSIC_TICKS_PER_SECOND = 35.0F;
    private static final float NORMAL_FLOOR_SPEED = DoomUnits.toWorld(1.0F) * CLASSIC_TICKS_PER_SECOND;

    /** Protects deterministic trigger ordering in the immutable publication. */
    DoomFloorPublication {
        triggerLinedefs = List.copyOf(triggerLinedefs);
    }

    /** Discovers one publication per tagged controlled sector in deterministic source-sector order. */
    static List<DoomFloorPublication> discover(DoomMap map) {
        DoomMap source = Objects.requireNonNull(map, "map");
        Map<Integer, List<Integer>> triggersBySector = new LinkedHashMap<>();
        for (int lineIndex = 0; lineIndex < source.linedefs().size(); lineIndex++) {
            DoomMap.Linedef linedef = source.linedefs().get(lineIndex);
            if (linedef.special() != WALK_ONCE_LOWER_TO_HIGHEST_SURROUNDING || linedef.tag() == 0) {
                continue;
            }
            for (int sectorIndex = 0; sectorIndex < source.sectors().size(); sectorIndex++) {
                if (source.sectors().get(sectorIndex).tag() == linedef.tag()) {
                    triggersBySector
                            .computeIfAbsent(sectorIndex, ignored -> new ArrayList<>())
                            .add(lineIndex);
                }
            }
        }
        return triggersBySector.entrySet().stream()
                .map(entry -> create(source, entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Calculates the exact type-19 destination and classic movement rate for one tagged sector. */
    private static DoomFloorPublication create(DoomMap map, int sectorIndex, List<Integer> triggerLinedefs) {
        float raised = floorHeight(map, sectorIndex);
        float lowered = highestAdjacentFloor(map, sectorIndex);
        if (!Float.isFinite(lowered) || lowered >= raised) {
            throw new IllegalArgumentException(
                    "type-19 floor sector has no lower surrounding destination: " + sectorIndex);
        }
        return new DoomFloorPublication(
                sectorIndex,
                DoomFloor.Profile.WALK_ONCE_LOWER_TO_HIGHEST_SURROUNDING,
                raised,
                lowered,
                NORMAL_FLOOR_SPEED,
                triggerLinedefs);
    }

    /** Finds the highest neighboring floor sharing a two-sided boundary with the controlled sector. */
    private static float highestAdjacentFloor(DoomMap map, int sectorIndex) {
        float adjacent = Float.NEGATIVE_INFINITY;
        for (DoomMap.Linedef linedef : map.linedefs()) {
            if (linedef.leftSidedef() < 0) {
                continue;
            }
            int right = sectorForSide(map, linedef.rightSidedef());
            int left = sectorForSide(map, linedef.leftSidedef());
            if (right == sectorIndex && left != sectorIndex) {
                adjacent = Math.max(adjacent, floorHeight(map, left));
            } else if (left == sectorIndex && right != sectorIndex) {
                adjacent = Math.max(adjacent, floorHeight(map, right));
            }
        }
        return adjacent;
    }

    private static float floorHeight(DoomMap map, int sectorIndex) {
        return DoomUnits.toWorld(map.sectors().get(sectorIndex).floorHeight());
    }

    private static int sectorForSide(DoomMap map, int sidedefIndex) {
        return map.sidedefs().get(sidedefIndex).sector();
    }
}
