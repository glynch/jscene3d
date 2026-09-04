/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.map;

import java.util.List;
import java.util.Objects;

/** Immutable, renderer-independent data decoded from one classic Doom map. */
public final class DoomMap {
    private final String name;
    private final List<Thing> things;
    private final Geometry geometry;
    private final Bsp bsp;
    private final List<Integer> rejectBytes;
    private final Blockmap blockmap;

    /**
     * Creates a decoded map from immutable domain groups.
     *
     * @param name normalized map marker
     * @param things map things in source order
     * @param geometry vertices, linedefs, sidedefs, and sectors
     * @param bsp segs, subsectors, and nodes
     * @param rejectBytes unsigned REJECT-table bytes
     * @param blockmap parsed collision blockmap
     */
    public DoomMap(
            String name, List<Thing> things, Geometry geometry, Bsp bsp, List<Integer> rejectBytes, Blockmap blockmap) {
        this.name = Objects.requireNonNull(name, "name");
        this.things = List.copyOf(things);
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        this.bsp = Objects.requireNonNull(bsp, "bsp");
        this.rejectBytes = List.copyOf(rejectBytes);
        this.blockmap = Objects.requireNonNull(blockmap, "blockmap");
    }

    /**
     * Returns the normalized map marker.
     *
     * @return uppercase map marker
     */
    public String name() {
        return name;
    }

    /**
     * Returns map things in lump order.
     *
     * @return immutable ordered things
     */
    public List<Thing> things() {
        return things;
    }

    /**
     * Returns linedefs in lump order.
     *
     * @return immutable ordered linedefs
     */
    public List<Linedef> linedefs() {
        return geometry.linedefs();
    }

    /**
     * Returns sidedefs in lump order.
     *
     * @return immutable ordered sidedefs
     */
    public List<Sidedef> sidedefs() {
        return geometry.sidedefs();
    }

    /**
     * Returns vertices in lump order.
     *
     * @return immutable ordered vertices
     */
    public List<Vertex> vertices() {
        return geometry.vertices();
    }

    /**
     * Returns segs in lump order.
     *
     * @return immutable ordered segs
     */
    public List<Seg> segs() {
        return bsp.segs();
    }

    /**
     * Returns subsectors in lump order.
     *
     * @return immutable ordered subsectors
     */
    public List<Subsector> subsectors() {
        return bsp.subsectors();
    }

    /**
     * Returns BSP nodes in lump order.
     *
     * @return immutable ordered BSP nodes
     */
    public List<Node> nodes() {
        return bsp.nodes();
    }

    /**
     * Returns sectors in lump order.
     *
     * @return immutable ordered sectors
     */
    public List<Sector> sectors() {
        return geometry.sectors();
    }

    /**
     * Returns unsigned REJECT-table bytes in source order.
     *
     * @return immutable values in the range 0 through 255
     */
    public List<Integer> rejectBytes() {
        return rejectBytes;
    }

    /**
     * Returns the parsed collision blockmap.
     *
     * @return immutable blockmap
     */
    public Blockmap blockmap() {
        return blockmap;
    }

    /**
     * Geometry and sector records used to construct the map.
     *
     * @param vertices vertices in source order
     * @param linedefs linedefs in source order
     * @param sidedefs sidedefs in source order
     * @param sectors sectors in source order
     */
    public record Geometry(
            List<Vertex> vertices, List<Linedef> linedefs, List<Sidedef> sidedefs, List<Sector> sectors) {
        /** Creates an immutable geometry group. */
        public Geometry {
            vertices = List.copyOf(vertices);
            linedefs = List.copyOf(linedefs);
            sidedefs = List.copyOf(sidedefs);
            sectors = List.copyOf(sectors);
        }
    }

    /**
     * BSP records used to construct the map.
     *
     * @param segs segs in source order
     * @param subsectors subsectors in source order
     * @param nodes nodes in source order
     */
    public record Bsp(List<Seg> segs, List<Subsector> subsectors, List<Node> nodes) {
        /** Creates an immutable BSP group. */
        public Bsp {
            segs = List.copyOf(segs);
            subsectors = List.copyOf(subsectors);
            nodes = List.copyOf(nodes);
        }
    }

    /**
     * A map thing placement.
     *
     * @param x signed horizontal coordinate
     * @param y signed vertical map coordinate
     * @param angle unsigned orientation in degrees
     * @param type unsigned Doom thing type
     * @param flags unsigned spawn flags
     */
    public record Thing(int x, int y, int angle, int type, int flags) {}

    /**
     * A boundary between two vertices with right and optional left sidedefs.
     *
     * @param startVertex start vertex index
     * @param endVertex end vertex index
     * @param flags unsigned linedef flags
     * @param special unsigned action special
     * @param tag unsigned sector tag
     * @param rightSidedef right sidedef index
     * @param leftSidedef left sidedef index, or {@code -1} when absent
     */
    public record Linedef(
            int startVertex, int endVertex, int flags, int special, int tag, int rightSidedef, int leftSidedef) {}

    /**
     * Material offsets and sector reference for one side of a linedef.
     *
     * @param xOffset signed horizontal texture offset
     * @param yOffset signed vertical texture offset
     * @param upperTexture upper texture name
     * @param lowerTexture lower texture name
     * @param middleTexture middle texture name
     * @param sector sector index
     */
    public record Sidedef(
            int xOffset, int yOffset, String upperTexture, String lowerTexture, String middleTexture, int sector) {
        /** Creates a sidedef with non-null normalized texture names. */
        public Sidedef {
            Objects.requireNonNull(upperTexture, "upperTexture");
            Objects.requireNonNull(lowerTexture, "lowerTexture");
            Objects.requireNonNull(middleTexture, "middleTexture");
        }
    }

    /**
     * A signed two-dimensional Doom map coordinate.
     *
     * @param x horizontal coordinate
     * @param y vertical map coordinate
     */
    public record Vertex(int x, int y) {}

    /**
     * A BSP segment derived from a linedef.
     *
     * @param startVertex start vertex index
     * @param endVertex end vertex index
     * @param angle unsigned binary angle
     * @param linedef linedef index
     * @param direction linedef direction, either zero or one
     * @param offset unsigned distance along the linedef
     */
    public record Seg(int startVertex, int endVertex, int angle, int linedef, int direction, int offset) {}

    /**
     * A contiguous range of BSP segments.
     *
     * @param segCount number of contiguous segs
     * @param firstSeg first seg index
     */
    public record Subsector(int segCount, int firstSeg) {}

    /**
     * One BSP partition node and its two bounded child branches.
     *
     * @param partition partition line
     * @param right right branch
     * @param left left branch
     */
    public record Node(Partition partition, NodeSide right, NodeSide left) {
        /** Creates a BSP node from non-null values. */
        public Node {
            Objects.requireNonNull(partition, "partition");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(left, "left");
        }
    }

    /**
     * Origin and direction vector of a BSP partition line.
     *
     * @param x origin x coordinate
     * @param y origin y coordinate
     * @param deltaX direction x component
     * @param deltaY direction y component
     */
    public record Partition(int x, int y, int deltaX, int deltaY) {}

    /**
     * Bounding box and child reference for one side of a BSP node.
     *
     * @param bounds child bounds
     * @param child child reference
     */
    public record NodeSide(BoundingBox bounds, NodeChild child) {
        /** Creates a bounded BSP branch from non-null values. */
        public NodeSide {
            Objects.requireNonNull(bounds, "bounds");
            Objects.requireNonNull(child, "child");
        }
    }

    /**
     * Top, bottom, left, and right bounds stored for a BSP child.
     *
     * @param top top coordinate
     * @param bottom bottom coordinate
     * @param left left coordinate
     * @param right right coordinate
     */
    public record BoundingBox(int top, int bottom, int left, int right) {}

    /**
     * A decoded BSP child reference.
     *
     * @param subsector whether the index references a subsector rather than a node
     * @param index node or subsector index
     */
    public record NodeChild(boolean subsector, int index) {}

    /**
     * Heights, materials, lighting, and behavior tag for a convex map region.
     *
     * @param floorHeight signed floor height
     * @param ceilingHeight signed ceiling height
     * @param floorTexture floor flat name
     * @param ceilingTexture ceiling flat name
     * @param lightLevel signed source light level
     * @param special unsigned sector special
     * @param tag unsigned sector tag
     */
    public record Sector(
            int floorHeight,
            int ceilingHeight,
            String floorTexture,
            String ceilingTexture,
            int lightLevel,
            int special,
            int tag) {
        /** Creates a sector with non-null normalized flat names. */
        public Sector {
            Objects.requireNonNull(floorTexture, "floorTexture");
            Objects.requireNonNull(ceilingTexture, "ceilingTexture");
        }
    }

    /**
     * Grid of linedef indexes used by classic Doom collision queries.
     *
     * @param originX signed grid origin x coordinate
     * @param originY signed grid origin y coordinate
     * @param columns unsigned column count
     * @param rows unsigned row count
     * @param cells row-major linedef-index lists
     */
    public record Blockmap(int originX, int originY, int columns, int rows, List<List<Integer>> cells) {
        /** Creates an immutable blockmap. */
        public Blockmap {
            cells = cells.stream().map(List::copyOf).toList();
        }

        /**
         * Returns the linedef indexes for one grid cell.
         *
         * @param column zero-based column
         * @param row zero-based row
         * @return immutable linedef indexes
         * @throws IndexOutOfBoundsException when the coordinates lie outside the grid
         */
        public List<Integer> cell(int column, int row) {
            if (column < 0 || column >= columns || row < 0 || row >= rows) {
                throw new IndexOutOfBoundsException("blockmap cell is outside the grid");
            }
            return cells.get(row * columns + column);
        }
    }
}
