/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.wad.WadArchive;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

/** Writes complete decoded maps as portable typed project resources. */
final class DoomMapResourceWriter {
    private static final JsonFactory JSON_FACTORY =
            JsonFactory.builder().disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();

    /** Prevents construction of this stateless serializer. */
    private DoomMapResourceWriter() {
        throw new AssertionError("DoomMapResourceWriter cannot be instantiated");
    }

    /** Writes one deterministic map resource without recording a machine-specific source path. */
    static void write(OutputStream output, String assetId, WadArchive archive, DoomMap map) throws IOException {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(archive, "archive");
        Objects.requireNonNull(map, "map");
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
            generator.setPrettyPrinter(prettyPrinter());
            generator.writeStartObject();
            generator.writeNumberField("schemaVersion", 1);
            generator.writeStringField("type", DoomImportExtension.MAP_RESOURCE_TYPE_IDENTIFIER);
            generator.writeNumberField("typeVersion", DoomImportExtension.TYPE_VERSION);
            generator.writeObjectFieldStart("properties");
            generator.writeStringField("name", map.name());
            writeSource(generator, assetId, archive);
            writeThings(generator, map.things());
            writeGeometry(generator, map);
            writeBsp(generator, map);
            writeReject(generator, map.rejectBytes());
            writeBlockmap(generator, map.blockmap());
            generator.writeEndObject();
            generator.writeEndObject();
            generator.writeRaw('\n');
        }
    }

    /** Creates the deterministic indentation policy for generated JSON artifacts. */
    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(indenter);
        prettyPrinter.indentArraysWith(indenter);
        return prettyPrinter;
    }

    /** Writes portable source provenance. */
    private static void writeSource(JsonGenerator generator, String assetId, WadArchive archive) throws IOException {
        generator.writeObjectFieldStart("source");
        generator.writeStringField("asset", assetId);
        generator.writeStringField("archiveKind", archive.kind().name());
        generator.writeNumberField("size", archive.provenance().fileSize());
        generator.writeStringField("sha256", archive.provenance().sha256());
        generator.writeEndObject();
    }

    /** Writes every thing in source order. */
    private static void writeThings(JsonGenerator generator, List<DoomMap.Thing> things) throws IOException {
        generator.writeArrayFieldStart("things");
        for (DoomMap.Thing thing : things) {
            generator.writeStartObject();
            generator.writeNumberField("x", thing.x());
            generator.writeNumberField("y", thing.y());
            generator.writeNumberField("angle", thing.angle());
            generator.writeNumberField("type", thing.type());
            generator.writeNumberField("flags", thing.flags());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    /** Writes source geometry tables without triangulation or coordinate conversion. */
    private static void writeGeometry(JsonGenerator generator, DoomMap map) throws IOException {
        generator.writeObjectFieldStart("geometry");
        writeVertices(generator, map.vertices());
        writeLinedefs(generator, map.linedefs());
        writeSidedefs(generator, map.sidedefs());
        writeSectors(generator, map.sectors());
        generator.writeEndObject();
    }

    /** Writes every vertex in source order. */
    private static void writeVertices(JsonGenerator generator, List<DoomMap.Vertex> vertices) throws IOException {
        generator.writeArrayFieldStart("vertices");
        for (DoomMap.Vertex vertex : vertices) {
            generator.writeStartObject();
            generator.writeNumberField("x", vertex.x());
            generator.writeNumberField("y", vertex.y());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    /** Writes every linedef in source order. */
    private static void writeLinedefs(JsonGenerator generator, List<DoomMap.Linedef> linedefs) throws IOException {
        generator.writeArrayFieldStart("linedefs");
        for (DoomMap.Linedef linedef : linedefs) {
            generator.writeStartObject();
            generator.writeNumberField("startVertex", linedef.startVertex());
            generator.writeNumberField("endVertex", linedef.endVertex());
            generator.writeNumberField("flags", linedef.flags());
            generator.writeNumberField("special", linedef.special());
            generator.writeNumberField("tag", linedef.tag());
            generator.writeNumberField("rightSidedef", linedef.rightSidedef());
            generator.writeNumberField("leftSidedef", linedef.leftSidedef());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    /** Writes every sidedef in source order. */
    private static void writeSidedefs(JsonGenerator generator, List<DoomMap.Sidedef> sidedefs) throws IOException {
        generator.writeArrayFieldStart("sidedefs");
        for (DoomMap.Sidedef sidedef : sidedefs) {
            generator.writeStartObject();
            generator.writeNumberField("xOffset", sidedef.xOffset());
            generator.writeNumberField("yOffset", sidedef.yOffset());
            generator.writeStringField("upperTexture", sidedef.upperTexture());
            generator.writeStringField("lowerTexture", sidedef.lowerTexture());
            generator.writeStringField("middleTexture", sidedef.middleTexture());
            generator.writeNumberField("sector", sidedef.sector());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    /** Writes every sector in source order. */
    private static void writeSectors(JsonGenerator generator, List<DoomMap.Sector> sectors) throws IOException {
        generator.writeArrayFieldStart("sectors");
        for (DoomMap.Sector sector : sectors) {
            generator.writeStartObject();
            generator.writeNumberField("floorHeight", sector.floorHeight());
            generator.writeNumberField("ceilingHeight", sector.ceilingHeight());
            generator.writeStringField("floorTexture", sector.floorTexture());
            generator.writeStringField("ceilingTexture", sector.ceilingTexture());
            generator.writeNumberField("lightLevel", sector.lightLevel());
            generator.writeNumberField("special", sector.special());
            generator.writeNumberField("tag", sector.tag());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    /** Writes source BSP tables without deriving renderer geometry. */
    private static void writeBsp(JsonGenerator generator, DoomMap map) throws IOException {
        generator.writeObjectFieldStart("bsp");
        writeSegs(generator, map.segs());
        writeSubsectors(generator, map.subsectors());
        writeNodes(generator, map.nodes());
        generator.writeEndObject();
    }

    /** Writes every BSP seg in source order. */
    private static void writeSegs(JsonGenerator generator, List<DoomMap.Seg> segs) throws IOException {
        generator.writeArrayFieldStart("segs");
        for (DoomMap.Seg seg : segs) {
            generator.writeStartObject();
            generator.writeNumberField("startVertex", seg.startVertex());
            generator.writeNumberField("endVertex", seg.endVertex());
            generator.writeNumberField("angle", seg.angle());
            generator.writeNumberField("linedef", seg.linedef());
            generator.writeNumberField("direction", seg.direction());
            generator.writeNumberField("offset", seg.offset());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    /** Writes every subsector in source order. */
    private static void writeSubsectors(JsonGenerator generator, List<DoomMap.Subsector> subsectors)
            throws IOException {
        generator.writeArrayFieldStart("subsectors");
        for (DoomMap.Subsector subsector : subsectors) {
            generator.writeStartObject();
            generator.writeNumberField("segCount", subsector.segCount());
            generator.writeNumberField("firstSeg", subsector.firstSeg());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    /** Writes every BSP node in source order. */
    private static void writeNodes(JsonGenerator generator, List<DoomMap.Node> nodes) throws IOException {
        generator.writeArrayFieldStart("nodes");
        for (DoomMap.Node node : nodes) {
            generator.writeStartObject();
            writePartition(generator, node.partition());
            writeNodeSide(generator, "right", node.right());
            writeNodeSide(generator, "left", node.left());
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }

    /** Writes one BSP partition line. */
    private static void writePartition(JsonGenerator generator, DoomMap.Partition partition) throws IOException {
        generator.writeObjectFieldStart("partition");
        generator.writeNumberField("x", partition.x());
        generator.writeNumberField("y", partition.y());
        generator.writeNumberField("deltaX", partition.deltaX());
        generator.writeNumberField("deltaY", partition.deltaY());
        generator.writeEndObject();
    }

    /** Writes one bounded BSP branch. */
    private static void writeNodeSide(JsonGenerator generator, String field, DoomMap.NodeSide side) throws IOException {
        generator.writeObjectFieldStart(field);
        DoomMap.BoundingBox bounds = side.bounds();
        generator.writeObjectFieldStart("bounds");
        generator.writeNumberField("top", bounds.top());
        generator.writeNumberField("bottom", bounds.bottom());
        generator.writeNumberField("left", bounds.left());
        generator.writeNumberField("right", bounds.right());
        generator.writeEndObject();
        generator.writeObjectFieldStart("child");
        generator.writeBooleanField("subsector", side.child().subsector());
        generator.writeNumberField("index", side.child().index());
        generator.writeEndObject();
        generator.writeEndObject();
    }

    /** Writes unsigned REJECT-table bytes in source order. */
    private static void writeReject(JsonGenerator generator, List<Integer> rejectBytes) throws IOException {
        generator.writeArrayFieldStart("reject");
        for (int value : rejectBytes) {
            generator.writeNumber(value);
        }
        generator.writeEndArray();
    }

    /** Writes the parsed collision blockmap and its row-major cell lists. */
    private static void writeBlockmap(JsonGenerator generator, DoomMap.Blockmap blockmap) throws IOException {
        generator.writeObjectFieldStart("blockmap");
        generator.writeNumberField("originX", blockmap.originX());
        generator.writeNumberField("originY", blockmap.originY());
        generator.writeNumberField("columns", blockmap.columns());
        generator.writeNumberField("rows", blockmap.rows());
        generator.writeArrayFieldStart("cells");
        for (List<Integer> cell : blockmap.cells()) {
            generator.writeStartArray();
            for (int linedef : cell) {
                generator.writeNumber(linedef);
            }
            generator.writeEndArray();
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }
}
