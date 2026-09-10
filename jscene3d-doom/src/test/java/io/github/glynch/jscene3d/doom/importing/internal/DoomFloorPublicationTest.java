/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.doom.runtime.DoomFloor;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Specifies conversion of type-19 tagged floors into descriptor-ready source semantics. */
final class DoomFloorPublicationTest {
    /** Resolves the tagged sector, highest surrounding floor, classic speed, and source trigger. */
    @Test
    void discoversWalkOnceLowerFloor() {
        DoomMap map = map(
                List.of(linedef(19, 7, 0, 2), linedef(0, 0, 1, 0), linedef(0, 0, 1, 2)),
                List.of(side(0), side(1), side(2)),
                List.of(sector(0, 128, 0), sector(64, 128, 7), sector(32, 128, 0)));

        assertThat(DoomFloorPublication.discover(map)).singleElement().satisfies(floor -> {
            assertThat(floor.sectorIndex()).isEqualTo(1);
            assertThat(floor.profile()).isEqualTo(DoomFloor.Profile.WALK_ONCE_LOWER_TO_HIGHEST_SURROUNDING);
            assertThat(floor.raisedHeight()).isEqualTo(2.0F);
            assertThat(floor.loweredHeight()).isEqualTo(1.0F);
            assertThat(floor.speed()).isEqualTo(1.09375F);
            assertThat(floor.triggerLinedefs()).containsExactly(0);
        });
    }

    /** Ignores unrelated specials and type-19 lines without the required sector tag. */
    @Test
    void ignoresUnsupportedAndUntaggedLinedefs() {
        DoomMap map = map(
                List.of(linedef(19, 0, 0, 1), linedef(62, 7, 0, 1)),
                List.of(side(0), side(1)),
                List.of(sector(0, 128, 0), sector(64, 128, 7)));

        assertThat(DoomFloorPublication.discover(map)).isEmpty();
    }

    /** Rejects invalid source data which does not describe a genuinely lower destination. */
    @Test
    void rejectsFloorWithoutLowerDestination() {
        DoomMap map = map(
                List.of(linedef(19, 7, 0, 1), linedef(0, 0, 1, 0)),
                List.of(side(0), side(1)),
                List.of(sector(64, 128, 0), sector(64, 128, 7)));

        assertThatThrownBy(() -> DoomFloorPublication.discover(map))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no lower surrounding destination");
    }

    private static DoomMap.Linedef linedef(int special, int tag, int rightSide, int leftSide) {
        return new DoomMap.Linedef(0, 1, 0, special, tag, rightSide, leftSide);
    }

    private static DoomMap.Sidedef side(int sector) {
        return new DoomMap.Sidedef(0, 0, "-", "-", "-", sector);
    }

    private static DoomMap.Sector sector(int floor, int ceiling, int tag) {
        return new DoomMap.Sector(floor, ceiling, "FLOOR", "CEILING", 160, 0, tag);
    }

    private static DoomMap map(
            List<DoomMap.Linedef> linedefs, List<DoomMap.Sidedef> sidedefs, List<DoomMap.Sector> sectors) {
        return new DoomMap(
                "MAP01",
                List.of(),
                new DoomMap.Geometry(List.of(), linedefs, sidedefs, sectors),
                new DoomMap.Bsp(List.of(), List.of(), List.of()),
                List.of(),
                new DoomMap.Blockmap(0, 0, 1, 1, List.of(List.of())));
    }
}
