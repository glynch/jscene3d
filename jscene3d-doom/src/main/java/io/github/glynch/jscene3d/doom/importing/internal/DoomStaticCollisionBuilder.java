/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import io.github.glynch.jscene3d.doom.importing.internal.DoomBspGeometry.PlanarPoint;
import io.github.glynch.jscene3d.doom.importing.internal.DoomBspGeometry.SubsectorPolygon;
import io.github.glynch.jscene3d.doom.importing.internal.TriangleMeshAccumulator.Point3;
import io.github.glynch.jscene3d.doom.map.DoomCoordinates;
import io.github.glynch.jscene3d.doom.map.DoomMap;
import java.util.List;
import java.util.Objects;

/** Derives static JScene3D triangle collision from decoded classic Doom geometry. */
final class DoomStaticCollisionBuilder {
    private static final int BLOCKING_LINE = 0x0001;

    /** Prevents construction of this stateless geometry adapter. */
    private DoomStaticCollisionBuilder() {
        throw new AssertionError("DoomStaticCollisionBuilder cannot be instantiated");
    }

    /** Builds floors, ceilings, solid boundaries, and portal height transitions. */
    static DoomStaticCollisionMesh build(DoomMap map) {
        DoomMap validMap = Objects.requireNonNull(map, "map");
        TriangleMeshAccumulator mesh = new TriangleMeshAccumulator();
        addPlanes(validMap, mesh);
        addWalls(validMap, mesh);
        return mesh.build();
    }

    /** Adds horizontal floors and ceilings for every recovered BSP leaf. */
    private static void addPlanes(DoomMap map, TriangleMeshAccumulator mesh) {
        for (SubsectorPolygon polygon : DoomBspGeometry.polygons(map)) {
            if (polygon.vertices().size() < 3) {
                continue;
            }
            DoomMap.Sector sector = map.sectors().get(polygon.sectorIndex());
            addPlane(mesh, polygon.vertices(), sector.floorHeight());
            addPlane(mesh, polygon.vertices(), sector.ceilingHeight());
        }
    }

    /** Triangulates one convex horizontal plane as a fan. */
    private static void addPlane(TriangleMeshAccumulator mesh, List<PlanarPoint> vertices, int height) {
        Point3 origin = point(vertices.getFirst(), height);
        for (int index = 1; index + 1 < vertices.size(); index++) {
            mesh.addTriangle(origin, point(vertices.get(index), height), point(vertices.get(index + 1), height));
        }
    }

    /** Adds one-sided walls and the closed portions of two-sided portal boundaries. */
    private static void addWalls(DoomMap map, TriangleMeshAccumulator mesh) {
        for (DoomMap.Linedef linedef : map.linedefs()) {
            DoomMap.Sector right = sector(map, linedef.rightSidedef());
            if (linedef.leftSidedef() < 0) {
                addWall(map, mesh, linedef, right.floorHeight(), right.ceilingHeight());
            } else {
                addPortalWalls(map, mesh, linedef, right, sector(map, linedef.leftSidedef()));
            }
        }
    }

    /** Adds floor, ceiling, and explicitly blocking spans across one portal. */
    private static void addPortalWalls(
            DoomMap map,
            TriangleMeshAccumulator mesh,
            DoomMap.Linedef linedef,
            DoomMap.Sector right,
            DoomMap.Sector left) {
        int lowerBottom = Math.min(right.floorHeight(), left.floorHeight());
        int lowerTop = Math.max(right.floorHeight(), left.floorHeight());
        addWall(map, mesh, linedef, lowerBottom, lowerTop);

        int upperBottom = Math.min(right.ceilingHeight(), left.ceilingHeight());
        int upperTop = Math.max(right.ceilingHeight(), left.ceilingHeight());
        addWall(map, mesh, linedef, upperBottom, upperTop);

        if ((linedef.flags() & BLOCKING_LINE) != 0) {
            int openingBottom = Math.max(right.floorHeight(), left.floorHeight());
            int openingTop = Math.min(right.ceilingHeight(), left.ceilingHeight());
            addWall(map, mesh, linedef, openingBottom, openingTop);
        }
    }

    /** Adds two triangles spanning one non-empty vertical linedef range. */
    private static void addWall(
            DoomMap map, TriangleMeshAccumulator mesh, DoomMap.Linedef linedef, int bottom, int top) {
        if (top <= bottom) {
            return;
        }
        DoomMap.Vertex start = map.vertices().get(linedef.startVertex());
        DoomMap.Vertex end = map.vertices().get(linedef.endVertex());
        Point3 startBottom = point(start, bottom);
        Point3 endBottom = point(end, bottom);
        Point3 endTop = point(end, top);
        Point3 startTop = point(start, top);
        mesh.addTriangle(startBottom, endBottom, endTop);
        mesh.addTriangle(startBottom, endTop, startTop);
    }

    /** Resolves the sector referenced by one validated sidedef. */
    private static DoomMap.Sector sector(DoomMap map, int sidedefIndex) {
        return map.sectors().get(map.sidedefs().get(sidedefIndex).sector());
    }

    /** Converts one planar source point and height to right-handed world coordinates. */
    private static Point3 point(PlanarPoint point, int height) {
        return new Point3(
                DoomCoordinates.toWorld((float) point.x()),
                DoomCoordinates.toWorld(height),
                DoomCoordinates.yToWorldZ(point.y()));
    }

    /** Converts one integral source vertex and height to right-handed world coordinates. */
    private static Point3 point(DoomMap.Vertex point, int height) {
        return new Point3(
                DoomCoordinates.toWorld(point.x()),
                DoomCoordinates.toWorld(height),
                DoomCoordinates.yToWorldZ(point.y()));
    }
}
