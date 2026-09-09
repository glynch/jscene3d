/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.doom.map.DoomMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Specifies conversion of supported classic door specials into descriptor-ready runtime values. */
final class DoomDoorPublicationTest {
    /** Discovers one publication per controlled sector in deterministic linedef order. */
    @Test
    void discoversManualAndBlazeDoors() {
        DoomMap map = map(
                List.of(linedef(31, 0, 1), linedef(31, 0, 1), linedef(117, 0, 2), linedef(1, 0, 2)),
                List.of(side(0), side(1), side(2)),
                List.of(sector(0, 128), sector(-64, 0), sector(-64, 32)));

        List<DoomDoorPublication> doors = DoomDoorPublication.discover(map);

        assertThat(doors).hasSize(2);
        assertThat(doors.get(0).sectorIndex()).isEqualTo(1);
        assertThat(doors.get(0).closedHeight()).isEqualTo(0.0F);
        assertThat(doors.get(0).openHeight()).isEqualTo(3.875F);
        assertThat(doors.get(0).speed()).isEqualTo(2.1875F);
        assertThat(doors.get(0).holdOpenSeconds()).isZero();
        assertThat(doors.get(1).sectorIndex()).isEqualTo(2);
        assertThat(doors.get(1).closedHeight()).isEqualTo(1.0F);
        assertThat(doors.get(1).openHeight()).isEqualTo(3.875F);
        assertThat(doors.get(1).speed()).isEqualTo(8.75F);
        assertThat(doors.get(1).holdOpenSeconds()).isCloseTo(150.0F / 35.0F, offset(1.0E-6F));
    }

    /** Ignores unsupported and one-sided linedefs because neither declares a supported door participant. */
    @Test
    void ignoresNonDoorLinedefs() {
        DoomMap map = map(
                List.of(new DoomMap.Linedef(0, 1, 0, 31, 0, 0, -1), linedef(63, 0, 1)),
                List.of(side(0), side(1)),
                List.of(sector(0, 128), sector(-64, 0)));

        assertThat(DoomDoorPublication.discover(map)).isEmpty();
    }

    /** Rejects a controlled sector without a higher adjacent ceiling rather than publishing invalid motion. */
    @Test
    void rejectsDoorWithoutOpenDestination() {
        DoomMap map = map(List.of(linedef(31, 0, 1)), List.of(side(0), side(1)), List.of(sector(0, 0), sector(-64, 0)));

        assertThatThrownBy(() -> DoomDoorPublication.discover(map))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no valid open destination");
    }

    private static DoomMap.Linedef linedef(int special, int rightSide, int leftSide) {
        return new DoomMap.Linedef(0, 1, 0, special, 0, rightSide, leftSide);
    }

    private static DoomMap.Sidedef side(int sector) {
        return new DoomMap.Sidedef(0, 0, "-", "-", "-", sector);
    }

    private static DoomMap.Sector sector(int floor, int ceiling) {
        return new DoomMap.Sector(floor, ceiling, "FLOOR", "CEILING", 160, 0, 0);
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
