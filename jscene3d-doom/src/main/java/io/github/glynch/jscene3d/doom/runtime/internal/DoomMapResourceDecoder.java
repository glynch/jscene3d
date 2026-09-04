/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import static io.github.glynch.jscene3d.project.runtime.extension.ProjectValues.array;
import static io.github.glynch.jscene3d.project.runtime.extension.ProjectValues.bool;
import static io.github.glynch.jscene3d.project.runtime.extension.ProjectValues.integer;
import static io.github.glynch.jscene3d.project.runtime.extension.ProjectValues.object;
import static io.github.glynch.jscene3d.project.runtime.extension.ProjectValues.required;
import static io.github.glynch.jscene3d.project.runtime.extension.ProjectValues.text;

import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactoryContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Decodes the Doom extension's portable map resource into its runtime domain value. */
final class DoomMapResourceDecoder {
    /** Prevents construction of this stateless resource decoder. */
    private DoomMapResourceDecoder() {
        throw new AssertionError("DoomMapResourceDecoder cannot be instantiated");
    }

    /** Creates one immutable runtime map from effective resource properties. */
    static DoomMap decode(ResourceFactoryContext context) {
        Map<String, ProjectValue> properties = context.properties();
        String name = text(required(properties, "name", "properties"), "properties/name");
        List<DoomMap.Thing> things = things(required(properties, "things", "properties"));
        DoomMap.Geometry geometry = geometry(required(properties, "geometry", "properties"));
        DoomMap.Bsp bsp = bsp(required(properties, "bsp", "properties"));
        List<Integer> reject = integers(required(properties, "reject", "properties"), "properties/reject");
        DoomMap.Blockmap blockmap = blockmap(required(properties, "blockmap", "properties"));
        return new DoomMap(name, things, geometry, bsp, reject, blockmap);
    }

    /** Decodes map things in source order. */
    private static List<DoomMap.Thing> things(ProjectValue value) {
        List<ProjectValue> values = array(value, "properties/things");
        List<DoomMap.Thing> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String location = "properties/things/" + index;
            Map<String, ProjectValue> thing = object(values.get(index), location);
            result.add(new DoomMap.Thing(
                    intProperty(thing, "x", location),
                    intProperty(thing, "y", location),
                    intProperty(thing, "angle", location),
                    intProperty(thing, "type", location),
                    intProperty(thing, "flags", location)));
        }
        return List.copyOf(result);
    }

    /** Decodes map geometry tables. */
    private static DoomMap.Geometry geometry(ProjectValue value) {
        String location = "properties/geometry";
        Map<String, ProjectValue> geometry = object(value, location);
        return new DoomMap.Geometry(
                vertices(required(geometry, "vertices", location)),
                linedefs(required(geometry, "linedefs", location)),
                sidedefs(required(geometry, "sidedefs", location)),
                sectors(required(geometry, "sectors", location)));
    }

    /** Decodes map vertices in source order. */
    private static List<DoomMap.Vertex> vertices(ProjectValue value) {
        String base = "properties/geometry/vertices";
        List<ProjectValue> values = array(value, base);
        List<DoomMap.Vertex> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String location = base + '/' + index;
            Map<String, ProjectValue> vertex = object(values.get(index), location);
            result.add(new DoomMap.Vertex(intProperty(vertex, "x", location), intProperty(vertex, "y", location)));
        }
        return List.copyOf(result);
    }

    /** Decodes map linedefs in source order. */
    private static List<DoomMap.Linedef> linedefs(ProjectValue value) {
        String base = "properties/geometry/linedefs";
        List<ProjectValue> values = array(value, base);
        List<DoomMap.Linedef> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String location = base + '/' + index;
            Map<String, ProjectValue> linedef = object(values.get(index), location);
            result.add(new DoomMap.Linedef(
                    intProperty(linedef, "startVertex", location),
                    intProperty(linedef, "endVertex", location),
                    intProperty(linedef, "flags", location),
                    intProperty(linedef, "special", location),
                    intProperty(linedef, "tag", location),
                    intProperty(linedef, "rightSidedef", location),
                    intProperty(linedef, "leftSidedef", location)));
        }
        return List.copyOf(result);
    }

    /** Decodes map sidedefs in source order. */
    private static List<DoomMap.Sidedef> sidedefs(ProjectValue value) {
        String base = "properties/geometry/sidedefs";
        List<ProjectValue> values = array(value, base);
        List<DoomMap.Sidedef> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String location = base + '/' + index;
            Map<String, ProjectValue> sidedef = object(values.get(index), location);
            result.add(new DoomMap.Sidedef(
                    intProperty(sidedef, "xOffset", location),
                    intProperty(sidedef, "yOffset", location),
                    textProperty(sidedef, "upperTexture", location),
                    textProperty(sidedef, "lowerTexture", location),
                    textProperty(sidedef, "middleTexture", location),
                    intProperty(sidedef, "sector", location)));
        }
        return List.copyOf(result);
    }

    /** Decodes sectors in source order. */
    private static List<DoomMap.Sector> sectors(ProjectValue value) {
        String base = "properties/geometry/sectors";
        List<ProjectValue> values = array(value, base);
        List<DoomMap.Sector> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String location = base + '/' + index;
            Map<String, ProjectValue> sector = object(values.get(index), location);
            result.add(new DoomMap.Sector(
                    intProperty(sector, "floorHeight", location),
                    intProperty(sector, "ceilingHeight", location),
                    textProperty(sector, "floorTexture", location),
                    textProperty(sector, "ceilingTexture", location),
                    intProperty(sector, "lightLevel", location),
                    intProperty(sector, "special", location),
                    intProperty(sector, "tag", location)));
        }
        return List.copyOf(result);
    }

    /** Decodes the map's BSP tables. */
    private static DoomMap.Bsp bsp(ProjectValue value) {
        String location = "properties/bsp";
        Map<String, ProjectValue> bsp = object(value, location);
        return new DoomMap.Bsp(
                segs(required(bsp, "segs", location)),
                subsectors(required(bsp, "subsectors", location)),
                nodes(required(bsp, "nodes", location)));
    }

    /** Decodes BSP segments in source order. */
    private static List<DoomMap.Seg> segs(ProjectValue value) {
        String base = "properties/bsp/segs";
        List<ProjectValue> values = array(value, base);
        List<DoomMap.Seg> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String location = base + '/' + index;
            Map<String, ProjectValue> seg = object(values.get(index), location);
            result.add(new DoomMap.Seg(
                    intProperty(seg, "startVertex", location),
                    intProperty(seg, "endVertex", location),
                    intProperty(seg, "angle", location),
                    intProperty(seg, "linedef", location),
                    intProperty(seg, "direction", location),
                    intProperty(seg, "offset", location)));
        }
        return List.copyOf(result);
    }

    /** Decodes BSP subsectors in source order. */
    private static List<DoomMap.Subsector> subsectors(ProjectValue value) {
        String base = "properties/bsp/subsectors";
        List<ProjectValue> values = array(value, base);
        List<DoomMap.Subsector> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String location = base + '/' + index;
            Map<String, ProjectValue> subsector = object(values.get(index), location);
            result.add(new DoomMap.Subsector(
                    intProperty(subsector, "segCount", location), intProperty(subsector, "firstSeg", location)));
        }
        return List.copyOf(result);
    }

    /** Decodes BSP nodes in source order. */
    private static List<DoomMap.Node> nodes(ProjectValue value) {
        String base = "properties/bsp/nodes";
        List<ProjectValue> values = array(value, base);
        List<DoomMap.Node> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            String location = base + '/' + index;
            Map<String, ProjectValue> node = object(values.get(index), location);
            result.add(new DoomMap.Node(
                    partition(required(node, "partition", location), location + "/partition"),
                    nodeSide(required(node, "right", location), location + "/right"),
                    nodeSide(required(node, "left", location), location + "/left")));
        }
        return List.copyOf(result);
    }

    /** Decodes one BSP partition. */
    private static DoomMap.Partition partition(ProjectValue value, String location) {
        Map<String, ProjectValue> partition = object(value, location);
        return new DoomMap.Partition(
                intProperty(partition, "x", location),
                intProperty(partition, "y", location),
                intProperty(partition, "deltaX", location),
                intProperty(partition, "deltaY", location));
    }

    /** Decodes one bounded BSP branch. */
    private static DoomMap.NodeSide nodeSide(ProjectValue value, String location) {
        Map<String, ProjectValue> side = object(value, location);
        return new DoomMap.NodeSide(
                bounds(required(side, "bounds", location), location + "/bounds"),
                child(required(side, "child", location), location + "/child"));
    }

    /** Decodes one BSP bounding box. */
    private static DoomMap.BoundingBox bounds(ProjectValue value, String location) {
        Map<String, ProjectValue> bounds = object(value, location);
        return new DoomMap.BoundingBox(
                intProperty(bounds, "top", location),
                intProperty(bounds, "bottom", location),
                intProperty(bounds, "left", location),
                intProperty(bounds, "right", location));
    }

    /** Decodes one BSP child reference. */
    private static DoomMap.NodeChild child(ProjectValue value, String location) {
        Map<String, ProjectValue> child = object(value, location);
        return new DoomMap.NodeChild(boolProperty(child, "subsector", location), intProperty(child, "index", location));
    }

    /** Decodes the collision blockmap. */
    private static DoomMap.Blockmap blockmap(ProjectValue value) {
        String location = "properties/blockmap";
        Map<String, ProjectValue> blockmap = object(value, location);
        return new DoomMap.Blockmap(
                intProperty(blockmap, "originX", location),
                intProperty(blockmap, "originY", location),
                intProperty(blockmap, "columns", location),
                intProperty(blockmap, "rows", location),
                cells(required(blockmap, "cells", location)));
    }

    /** Decodes row-major blockmap cells. */
    private static List<List<Integer>> cells(ProjectValue value) {
        String base = "properties/blockmap/cells";
        List<ProjectValue> values = array(value, base);
        List<List<Integer>> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            result.add(integers(values.get(index), base + '/' + index));
        }
        return List.copyOf(result);
    }

    /** Decodes an integer array. */
    private static List<Integer> integers(ProjectValue value, String location) {
        List<ProjectValue> values = array(value, location);
        List<Integer> result = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            result.add(integer(values.get(index), location + '/' + index));
        }
        return List.copyOf(result);
    }

    /** Reads one required integer property. */
    private static int intProperty(Map<String, ProjectValue> values, String name, String location) {
        return integer(required(values, name, location), location + '/' + name);
    }

    /** Reads one required text property. */
    private static String textProperty(Map<String, ProjectValue> values, String name, String location) {
        return text(required(values, name, location), location + '/' + name);
    }

    /** Reads one required boolean property. */
    private static boolean boolProperty(Map<String, ProjectValue> values, String name, String location) {
        return bool(required(values, name, location), location + '/' + name);
    }
}
