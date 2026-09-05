/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import io.github.glynch.jscene3d.doom.map.DoomMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Recovers convex Doom subsector polygons from the map's BSP partition tree. */
final class DoomBspGeometry {
    private static final double CLIPPING_TOLERANCE = 0.000_001;

    /** Prevents construction of this stateless geometry adapter. */
    private DoomBspGeometry() {
        throw new AssertionError("DoomBspGeometry cannot be instantiated");
    }

    /** Returns one ordered convex polygon and owning sector for every subsector. */
    static List<SubsectorPolygon> polygons(DoomMap map) {
        DoomMap validMap = Objects.requireNonNull(map, "map");
        List<List<PlanarPoint>> polygons = recoverPolygons(validMap);
        List<SubsectorPolygon> result = new ArrayList<>(polygons.size());
        for (int index = 0; index < polygons.size(); index++) {
            result.add(new SubsectorPolygon(sectorForSubsector(validMap, index), polygons.get(index)));
        }
        return List.copyOf(result);
    }

    /** Recovers polygons directly from segs when no partition nodes exist. */
    private static List<List<PlanarPoint>> recoverPolygons(DoomMap map) {
        if (map.nodes().isEmpty()) {
            List<List<PlanarPoint>> polygons = new ArrayList<>(map.subsectors().size());
            for (int index = 0; index < map.subsectors().size(); index++) {
                polygons.add(segPolygon(map, index));
            }
            return List.copyOf(polygons);
        }
        List<List<PlanarPoint>> polygons = new ArrayList<>(map.subsectors().size());
        for (int index = 0; index < map.subsectors().size(); index++) {
            polygons.add(List.of());
        }
        DoomMap.NodeChild root = new DoomMap.NodeChild(false, map.nodes().size() - 1);
        recoverPolygons(map, root, mapBounds(map), polygons);
        return List.copyOf(polygons);
    }

    /** Recursively clips one convex region into the node's right and left children. */
    private static void recoverPolygons(
            DoomMap map, DoomMap.NodeChild child, List<PlanarPoint> polygon, List<List<PlanarPoint>> polygons) {
        if (child.subsector()) {
            polygons.set(child.index(), compactPolygon(polygon));
            return;
        }
        DoomMap.Node node = map.nodes().get(child.index());
        recoverPolygons(map, node.right().child(), clip(polygon, node.partition(), true), polygons);
        recoverPolygons(map, node.left().child(), clip(polygon, node.partition(), false), polygons);
    }

    /** Clips one convex polygon to a selected side of a BSP partition. */
    private static List<PlanarPoint> clip(List<PlanarPoint> polygon, DoomMap.Partition partition, boolean rightSide) {
        if (polygon.isEmpty()) {
            return polygon;
        }
        List<PlanarPoint> result = new ArrayList<>(polygon.size() + 1);
        PlanarPoint previous = polygon.getLast();
        double previousSide = partitionSide(partition, previous.x(), previous.y());
        boolean previousInside = inside(previousSide, rightSide);
        for (PlanarPoint current : polygon) {
            double currentSide = partitionSide(partition, current.x(), current.y());
            boolean currentInside = inside(currentSide, rightSide);
            if (currentInside != previousInside) {
                double amount = previousSide / (previousSide - currentSide);
                result.add(new PlanarPoint(
                        previous.x() + amount * (current.x() - previous.x()),
                        previous.y() + amount * (current.y() - previous.y())));
            }
            if (currentInside) {
                result.add(current);
            }
            previous = current;
            previousSide = currentSide;
            previousInside = currentInside;
        }
        return compactPolygon(result);
    }

    /** Uses classic BSP side zero for the negative partition half-plane. */
    private static boolean inside(double side, boolean rightSide) {
        return rightSide ? side <= CLIPPING_TOLERANCE : side >= -CLIPPING_TOLERANCE;
    }

    /** Calculates the partition cross product used for BSP side selection. */
    private static double partitionSide(DoomMap.Partition partition, double x, double y) {
        return partition.deltaX() * (y - partition.y()) - partition.deltaY() * (x - partition.x());
    }

    /** Returns a finite rectangle enclosing every explicit map vertex. */
    private static List<PlanarPoint> mapBounds(DoomMap map) {
        int minimumX = map.vertices().stream().mapToInt(DoomMap.Vertex::x).min().orElseThrow();
        int maximumX = map.vertices().stream().mapToInt(DoomMap.Vertex::x).max().orElseThrow();
        int minimumY = map.vertices().stream().mapToInt(DoomMap.Vertex::y).min().orElseThrow();
        int maximumY = map.vertices().stream().mapToInt(DoomMap.Vertex::y).max().orElseThrow();
        return List.of(
                new PlanarPoint(minimumX, minimumY),
                new PlanarPoint(maximumX, minimumY),
                new PlanarPoint(maximumX, maximumY),
                new PlanarPoint(minimumX, maximumY));
    }

    /** Returns stored seg starts for a subsector without a partition tree. */
    private static List<PlanarPoint> segPolygon(DoomMap map, int subsectorIndex) {
        DoomMap.Subsector subsector = map.subsectors().get(subsectorIndex);
        List<PlanarPoint> vertices = new ArrayList<>(subsector.segCount());
        for (int offset = 0; offset < subsector.segCount(); offset++) {
            DoomMap.Seg seg = map.segs().get(subsector.firstSeg() + offset);
            DoomMap.Vertex vertex = map.vertices().get(seg.startVertex());
            PlanarPoint point = new PlanarPoint(vertex.x(), vertex.y());
            if (vertices.isEmpty() || !vertices.getLast().equals(point)) {
                vertices.add(point);
            }
        }
        return compactPolygon(vertices);
    }

    /** Resolves the sector referenced by the first directed seg of a subsector. */
    private static int sectorForSubsector(DoomMap map, int subsectorIndex) {
        DoomMap.Subsector subsector = map.subsectors().get(subsectorIndex);
        DoomMap.Seg seg = map.segs().get(subsector.firstSeg());
        DoomMap.Linedef linedef = map.linedefs().get(seg.linedef());
        int sidedefIndex = seg.direction() == 0 ? linedef.rightSidedef() : linedef.leftSidedef();
        return map.sidedefs().get(sidedefIndex).sector();
    }

    /** Removes adjacent coincident points left by partition clipping. */
    private static List<PlanarPoint> compactPolygon(List<PlanarPoint> polygon) {
        List<PlanarPoint> compact = new ArrayList<>(polygon.size());
        for (PlanarPoint point : polygon) {
            if (compact.isEmpty() || !samePoint(compact.getLast(), point)) {
                compact.add(point);
            }
        }
        if (compact.size() > 1 && samePoint(compact.getFirst(), compact.getLast())) {
            compact.removeLast();
        }
        return List.copyOf(compact);
    }

    /** Compares clipping coordinates with the BSP tolerance. */
    private static boolean samePoint(PlanarPoint first, PlanarPoint second) {
        return Math.abs(first.x() - second.x()) < CLIPPING_TOLERANCE
                && Math.abs(first.y() - second.y()) < CLIPPING_TOLERANCE;
    }

    /** One point in the source map plane. */
    record PlanarPoint(double x, double y) {}

    /** One recovered convex BSP leaf and its owning sector. */
    record SubsectorPolygon(int sectorIndex, List<PlanarPoint> vertices) {
        /** Copies the recovered polygon. */
        SubsectorPolygon {
            vertices = List.copyOf(vertices);
        }
    }
}
