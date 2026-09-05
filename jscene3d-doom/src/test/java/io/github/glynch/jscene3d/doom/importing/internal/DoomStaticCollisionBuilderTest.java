/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Specifies conversion of Doom sector geometry to generic static collision triangles. */
final class DoomStaticCollisionBuilderTest {
    /** Builds floors, ceilings, and four walls for a closed square sector. */
    @Test
    void buildsClosedRoomCollision() {
        DoomStaticCollisionMesh mesh = DoomStaticCollisionBuilder.build(closedRoom());

        assertThat(mesh.triangleCount()).isEqualTo(12);
        assertThat(mesh.positions()).hasSize(108).contains(0.0F, 4.0F, -4.0F);
        assertThat(mesh.indices()).containsExactly(range(36));
        assertThat(new TriangleMeshShape(mesh.positions(), mesh.indices()).triangleCount())
                .isEqualTo(12);
    }

    /** Adds only the lower and upper closed spans of an unblocked two-sided portal. */
    @Test
    void buildsPortalHeightTransitions() {
        DoomStaticCollisionMesh mesh = DoomStaticCollisionBuilder.build(portal(false));

        assertThat(mesh.triangleCount()).isEqualTo(4);
    }

    /** Fills the remaining opening when a two-sided linedef is explicitly blocking. */
    @Test
    void buildsBlockingPortalOpening() {
        DoomStaticCollisionMesh mesh = DoomStaticCollisionBuilder.build(portal(true));

        assertThat(mesh.triangleCount()).isEqualTo(6);
    }

    /** Creates a closed room with one convex BSP leaf. */
    private static DoomMap closedRoom() {
        List<DoomMap.Vertex> vertices = List.of(
                new DoomMap.Vertex(0, 0),
                new DoomMap.Vertex(0, 128),
                new DoomMap.Vertex(128, 128),
                new DoomMap.Vertex(128, 0));
        List<DoomMap.Linedef> linedefs =
                List.of(line(0, 1, 0, -1, 0), line(1, 2, 1, -1, 0), line(2, 3, 2, -1, 0), line(3, 0, 3, -1, 0));
        List<DoomMap.Sidedef> sidedefs = List.of(side(0), side(0), side(0), side(0));
        List<DoomMap.Seg> segs = List.of(seg(0, 1, 0), seg(1, 2, 1), seg(2, 3, 2), seg(3, 0, 3));
        return map(vertices, linedefs, sidedefs, List.of(sector(0, 128)), segs, 4);
    }

    /** Creates a two-sector portal with no valid plane polygon. */
    private static DoomMap portal(boolean blocking) {
        List<DoomMap.Vertex> vertices = List.of(new DoomMap.Vertex(0, 0), new DoomMap.Vertex(128, 0));
        List<DoomMap.Linedef> linedefs = List.of(line(0, 1, 0, 1, blocking ? 1 : 0));
        return map(
                vertices,
                linedefs,
                List.of(side(0), side(1)),
                List.of(sector(0, 128), sector(32, 96)),
                List.of(seg(0, 1, 0)),
                1);
    }

    /** Creates a minimal decoded map from collision-relevant records. */
    private static DoomMap map(
            List<DoomMap.Vertex> vertices,
            List<DoomMap.Linedef> linedefs,
            List<DoomMap.Sidedef> sidedefs,
            List<DoomMap.Sector> sectors,
            List<DoomMap.Seg> segs,
            int segCount) {
        return new DoomMap(
                "MAP01",
                List.of(),
                new DoomMap.Geometry(vertices, linedefs, sidedefs, sectors),
                new DoomMap.Bsp(segs, List.of(new DoomMap.Subsector(segCount, 0)), List.of()),
                List.of(),
                new DoomMap.Blockmap(0, 0, 1, 1, List.of(List.of())));
    }

    /** Creates one linedef using supplied side indices and flags. */
    private static DoomMap.Linedef line(int start, int end, int right, int left, int flags) {
        return new DoomMap.Linedef(start, end, flags, 0, 0, right, left);
    }

    /** Creates one source seg directed like its linedef. */
    private static DoomMap.Seg seg(int start, int end, int linedef) {
        return new DoomMap.Seg(start, end, 0, linedef, 0, 0);
    }

    /** Creates one sidedef referencing a sector. */
    private static DoomMap.Sidedef side(int sector) {
        return new DoomMap.Sidedef(0, 0, "-", "-", "-", sector);
    }

    /** Creates one sector with supplied vertical bounds. */
    private static DoomMap.Sector sector(int floor, int ceiling) {
        return new DoomMap.Sector(floor, ceiling, "FLOOR", "CEILING", 160, 0, 0);
    }

    /** Creates consecutive indices from zero through the requested exclusive limit. */
    private static int[] range(int limit) {
        int[] result = new int[limit];
        for (int index = 0; index < limit; index++) {
            result[index] = index;
        }
        return result;
    }
}
