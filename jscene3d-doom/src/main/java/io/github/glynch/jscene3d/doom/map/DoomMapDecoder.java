/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.map;

import io.github.glynch.jscene3d.doom.diagnostic.DoomDiagnostic;
import io.github.glynch.jscene3d.doom.diagnostic.DoomDiagnosticCode;
import io.github.glynch.jscene3d.doom.internal.Preconditions;
import io.github.glynch.jscene3d.wad.WadArchive;
import io.github.glynch.jscene3d.wad.WadLump;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Discovers and decodes classic Doom binary maps without rendering or applying game rules.
 *
 * <p>Instances are stateless and thread-safe. Decoding reads each required map lump at most once and rejects an
 * individual lump larger than 64 MiB before allocating its content.
 */
public final class DoomMapDecoder {
    private static final int MAX_MAP_LUMP_BYTES = 64 * 1024 * 1024;
    private static final List<String> CLASSIC_LUMP_NAMES = List.of(
            "THINGS", "LINEDEFS", "SIDEDEFS", "VERTEXES", "SEGS", "SSECTORS", "NODES", "SECTORS", "REJECT", "BLOCKMAP");

    /** Creates a stateless classic Doom map decoder. */
    public DoomMapDecoder() {
        // Public construction provides the supported decoding interface.
    }

    /**
     * Discovers unique conventional map markers in directory order without reading lump content.
     *
     * <p>Names matching {@code MAP##} or {@code E#M#} are returned in uppercase. Discovery does not imply that the
     * following map layout is valid or supported; callers use {@link #decode(WadArchive, String)} for that validation.
     *
     * @param archive validated source archive
     * @return immutable ordered map-marker names
     */
    public List<String> discover(WadArchive archive) {
        Objects.requireNonNull(archive, "archive");
        Set<String> names = new LinkedHashSet<>();
        archive.lumps().stream()
                .map(WadLump::name)
                .filter(DoomMapDecoder::isMapMarker)
                .forEach(names::add);
        return List.copyOf(names);
    }

    /**
     * Decodes one named classic map through the validated WAD archive interface.
     *
     * <p>If a marker occurs more than once, the last occurrence is authoritative. Operational and content failures are
     * returned as typed diagnostics rather than thrown. Invalid caller arguments still throw standard contract
     * exceptions.
     *
     * @param archive validated source archive
     * @param mapName case-insensitive map marker
     * @return decoded map or ordered source-aware diagnostics
     */
    public DoomMapDecodeResult decode(WadArchive archive, String mapName) {
        WadArchive validArchive = Objects.requireNonNull(archive, "archive");
        String normalizedName =
                Preconditions.requireNonBlank(mapName, "mapName").toUpperCase(Locale.ROOT);
        List<DoomDiagnostic> diagnostics = new ArrayList<>();
        List<WadLump> mapLumps = findMapLumps(validArchive, normalizedName, diagnostics);
        if (mapLumps.isEmpty()) {
            return new DoomMapDecodeResult(Optional.empty(), diagnostics);
        }

        try {
            return decodeLumps(validArchive, normalizedName, mapLumps, diagnostics);
        } catch (DecodeFailure failure) {
            diagnostics.add(error(
                    validArchive,
                    failure.code(),
                    "/maps/" + normalizedName + "/" + failure.location(),
                    failure.details()));
        } catch (IOException | RuntimeException exception) {
            diagnostics.add(error(
                    validArchive,
                    DoomDiagnosticCode.MAP_DATA_UNREADABLE,
                    "/maps/" + normalizedName,
                    Map.of("technicalDetail", failureMessage(exception))));
        }
        return new DoomMapDecodeResult(Optional.empty(), diagnostics);
    }

    /** Decodes and validates every classic lump after layout resolution. */
    private static DoomMapDecodeResult decodeLumps(
            WadArchive archive, String mapName, List<WadLump> mapLumps, List<DoomDiagnostic> diagnostics)
            throws IOException {
        List<DoomMap.Thing> things = parseThings(read(archive, mapLumps, 0));
        List<DoomMap.Linedef> linedefs = parseLinedefs(read(archive, mapLumps, 1));
        List<DoomMap.Sidedef> sidedefs = parseSidedefs(read(archive, mapLumps, 2));
        List<DoomMap.Vertex> vertices = parseVertices(read(archive, mapLumps, 3));
        List<DoomMap.Seg> segs = parseSegs(read(archive, mapLumps, 4));
        List<DoomMap.Subsector> subsectors = parseSubsectors(read(archive, mapLumps, 5));
        List<DoomMap.Node> nodes = parseNodes(read(archive, mapLumps, 6));
        List<DoomMap.Sector> sectors = parseSectors(read(archive, mapLumps, 7));
        List<Integer> rejectBytes = unsignedBytes(read(archive, mapLumps, 8));
        DoomMap.Blockmap blockmap = parseBlockmap(read(archive, mapLumps, 9));
        validateLinedefs(linedefs, vertices.size(), sidedefs.size());
        validateSidedefs(sidedefs, sectors.size());
        validateSegs(segs, vertices.size(), linedefs.size());
        validateSubsectors(subsectors, segs.size());
        validateNodes(nodes, subsectors.size());
        validateReject(rejectBytes, sectors.size());
        validateBlockmap(blockmap, linedefs.size());
        DoomMap map = new DoomMap(
                mapName,
                things,
                new DoomMap.Geometry(vertices, linedefs, sidedefs, sectors),
                new DoomMap.Bsp(segs, subsectors, nodes),
                rejectBytes,
                blockmap);
        return new DoomMapDecodeResult(Optional.of(map), diagnostics);
    }

    /** Resolves and validates the required ordered classic map lumps. */
    private static List<WadLump> findMapLumps(WadArchive archive, String mapName, List<DoomDiagnostic> diagnostics) {
        int markerIndex = lastMarkerIndex(archive, mapName);
        if (markerIndex < 0) {
            diagnostics.add(error(archive, DoomDiagnosticCode.MAP_MISSING, "/maps/" + mapName, Map.of("map", mapName)));
            return List.of();
        }
        if (isFollowingLump(archive, markerIndex, "TEXTMAP")) {
            diagnostics.add(error(
                    archive,
                    DoomDiagnosticCode.MAP_FORMAT_UDMF_UNSUPPORTED,
                    "/maps/" + mapName + "/TEXTMAP",
                    Map.of("map", mapName)));
            return List.of();
        }
        if (markerIndex + CLASSIC_LUMP_NAMES.size() >= archive.lumps().size()) {
            diagnostics.add(
                    error(archive, DoomDiagnosticCode.MAP_LAYOUT_INVALID, "/maps/" + mapName, Map.of("map", mapName)));
            return List.of();
        }

        List<WadLump> result = orderedClassicLumps(archive, mapName, markerIndex, diagnostics);
        if (result.isEmpty()) {
            return result;
        }
        int followingIndex = markerIndex + CLASSIC_LUMP_NAMES.size() + 1;
        if (followingIndex < archive.lumps().size()
                && archive.lumps().get(followingIndex).name().equals("BEHAVIOR")) {
            diagnostics.add(error(
                    archive,
                    DoomDiagnosticCode.MAP_FORMAT_HEXEN_UNSUPPORTED,
                    "/maps/" + mapName + "/BEHAVIOR",
                    Map.of("map", mapName)));
            return List.of();
        }
        return result;
    }

    /** Finds the final marker occurrence used by Doom replacement semantics. */
    private static int lastMarkerIndex(WadArchive archive, String mapName) {
        int markerIndex = -1;
        for (WadLump lump : archive.lumps()) {
            if (lump.name().equals(mapName)) {
                markerIndex = lump.index();
            }
        }
        return markerIndex;
    }

    /** Reports whether the first lump after a marker has the requested name. */
    private static boolean isFollowingLump(WadArchive archive, int markerIndex, String name) {
        int index = markerIndex + 1;
        return index < archive.lumps().size()
                && archive.lumps().get(index).name().equals(name);
    }

    /** Collects the exact required sequence or appends its first layout diagnostic. */
    private static List<WadLump> orderedClassicLumps(
            WadArchive archive, String mapName, int markerIndex, List<DoomDiagnostic> diagnostics) {
        List<WadLump> result = new ArrayList<>(CLASSIC_LUMP_NAMES.size());
        for (int offset = 0; offset < CLASSIC_LUMP_NAMES.size(); offset++) {
            WadLump lump = archive.lumps().get(markerIndex + offset + 1);
            String expected = CLASSIC_LUMP_NAMES.get(offset);
            if (!lump.name().equals(expected)) {
                diagnostics.add(error(
                        archive,
                        DoomDiagnosticCode.MAP_LAYOUT_INVALID,
                        "/maps/" + mapName + "/" + expected,
                        Map.of("actualLump", lump.name(), "expectedLump", expected)));
                return List.of();
            }
            result.add(lump);
        }
        return List.copyOf(result);
    }

    /** Reads one bounded lump selected by sequence position. */
    private static byte[] read(WadArchive archive, List<WadLump> lumps, int index) throws IOException {
        WadLump lump = lumps.get(index);
        if (lump.size() > MAX_MAP_LUMP_BYTES) {
            throw new DecodeFailure(
                    DoomDiagnosticCode.MAP_DATA_UNREADABLE,
                    lump.name(),
                    Map.of(
                            "maximumBytes", Integer.toString(MAX_MAP_LUMP_BYTES),
                            "size", Integer.toString(lump.size())));
        }
        return archive.readAllBytes(lump, MAX_MAP_LUMP_BYTES);
    }

    /** Parses ten-byte THINGS records. */
    private static List<DoomMap.Thing> parseThings(byte[] data) {
        ByteBuffer input = records(data, 10, "THINGS");
        List<DoomMap.Thing> result = new ArrayList<>(data.length / 10);
        while (input.hasRemaining()) {
            result.add(new DoomMap.Thing(
                    input.getShort(), input.getShort(), unsigned(input), unsigned(input), unsigned(input)));
        }
        return List.copyOf(result);
    }

    /** Parses fourteen-byte LINEDEFS records. */
    private static List<DoomMap.Linedef> parseLinedefs(byte[] data) {
        ByteBuffer input = records(data, 14, "LINEDEFS");
        List<DoomMap.Linedef> result = new ArrayList<>(data.length / 14);
        while (input.hasRemaining()) {
            result.add(new DoomMap.Linedef(
                    unsigned(input),
                    unsigned(input),
                    unsigned(input),
                    unsigned(input),
                    unsigned(input),
                    indexOrMissing(input),
                    indexOrMissing(input)));
        }
        return List.copyOf(result);
    }

    /** Parses thirty-byte SIDEDEFS records. */
    private static List<DoomMap.Sidedef> parseSidedefs(byte[] data) {
        ByteBuffer input = records(data, 30, "SIDEDEFS");
        List<DoomMap.Sidedef> result = new ArrayList<>(data.length / 30);
        while (input.hasRemaining()) {
            result.add(new DoomMap.Sidedef(
                    input.getShort(), input.getShort(), name(input), name(input), name(input), unsigned(input)));
        }
        return List.copyOf(result);
    }

    /** Parses four-byte VERTEXES records. */
    private static List<DoomMap.Vertex> parseVertices(byte[] data) {
        ByteBuffer input = records(data, 4, "VERTEXES");
        List<DoomMap.Vertex> result = new ArrayList<>(data.length / 4);
        while (input.hasRemaining()) {
            result.add(new DoomMap.Vertex(input.getShort(), input.getShort()));
        }
        return List.copyOf(result);
    }

    /** Parses twelve-byte SEGS records. */
    private static List<DoomMap.Seg> parseSegs(byte[] data) {
        ByteBuffer input = records(data, 12, "SEGS");
        List<DoomMap.Seg> result = new ArrayList<>(data.length / 12);
        while (input.hasRemaining()) {
            result.add(new DoomMap.Seg(
                    unsigned(input),
                    unsigned(input),
                    unsigned(input),
                    unsigned(input),
                    unsigned(input),
                    unsigned(input)));
        }
        return List.copyOf(result);
    }

    /** Parses four-byte SSECTORS records. */
    private static List<DoomMap.Subsector> parseSubsectors(byte[] data) {
        ByteBuffer input = records(data, 4, "SSECTORS");
        List<DoomMap.Subsector> result = new ArrayList<>(data.length / 4);
        while (input.hasRemaining()) {
            result.add(new DoomMap.Subsector(unsigned(input), unsigned(input)));
        }
        return List.copyOf(result);
    }

    /** Parses twenty-eight-byte NODES records. */
    private static List<DoomMap.Node> parseNodes(byte[] data) {
        ByteBuffer input = records(data, 28, "NODES");
        List<DoomMap.Node> result = new ArrayList<>(data.length / 28);
        while (input.hasRemaining()) {
            DoomMap.Partition partition =
                    new DoomMap.Partition(input.getShort(), input.getShort(), input.getShort(), input.getShort());
            DoomMap.BoundingBox right = boundingBox(input);
            DoomMap.BoundingBox left = boundingBox(input);
            DoomMap.NodeChild rightChild = child(input);
            DoomMap.NodeChild leftChild = child(input);
            result.add(new DoomMap.Node(
                    partition, new DoomMap.NodeSide(right, rightChild), new DoomMap.NodeSide(left, leftChild)));
        }
        return List.copyOf(result);
    }

    /** Parses twenty-six-byte SECTORS records. */
    private static List<DoomMap.Sector> parseSectors(byte[] data) {
        ByteBuffer input = records(data, 26, "SECTORS");
        List<DoomMap.Sector> result = new ArrayList<>(data.length / 26);
        while (input.hasRemaining()) {
            result.add(new DoomMap.Sector(
                    input.getShort(),
                    input.getShort(),
                    name(input),
                    name(input),
                    input.getShort(),
                    unsigned(input),
                    unsigned(input)));
        }
        return List.copyOf(result);
    }

    /** Parses a complete classic BLOCKMAP. */
    private static DoomMap.Blockmap parseBlockmap(byte[] data) {
        ByteBuffer input = blockmapInput(data);
        int originX = input.getShort();
        int originY = input.getShort();
        int columns = unsigned(input);
        int rows = unsigned(input);
        int[] offsets = blockmapOffsets(input, blockmapCellCount(columns, rows));
        List<List<Integer>> cells = new ArrayList<>(offsets.length);
        for (int index = 0; index < offsets.length; index++) {
            cells.add(blockmapCell(input, data.length, offsets[index], index));
        }
        return new DoomMap.Blockmap(originX, originY, columns, rows, cells);
    }

    /** Validates BLOCKMAP word alignment and returns little-endian input. */
    private static ByteBuffer blockmapInput(byte[] data) {
        if (data.length < 8 || data.length % Short.BYTES != 0) {
            throw new DecodeFailure(
                    DoomDiagnosticCode.MAP_BLOCKMAP_INVALID, "BLOCKMAP", Map.of("size", Integer.toString(data.length)));
        }
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    /** Calculates the block count without integer overflow. */
    private static int blockmapCellCount(int columns, int rows) {
        try {
            return Math.multiplyExact(columns, rows);
        } catch (ArithmeticException exception) {
            throw new DecodeFailure(
                    DoomDiagnosticCode.MAP_BLOCKMAP_INVALID,
                    "BLOCKMAP",
                    Map.of("columns", Integer.toString(columns), "rows", Integer.toString(rows)));
        }
    }

    /** Reads the complete BLOCKMAP cell-offset table. */
    private static int[] blockmapOffsets(ByteBuffer input, int cellCount) {
        if (cellCount > input.remaining() / Short.BYTES) {
            throw new DecodeFailure(
                    DoomDiagnosticCode.MAP_BLOCKMAP_INVALID,
                    "BLOCKMAP",
                    Map.of("cellCount", Integer.toString(cellCount)));
        }
        int[] offsets = new int[cellCount];
        for (int index = 0; index < cellCount; index++) {
            offsets[index] = unsigned(input);
        }
        return offsets;
    }

    /** Reads one terminated BLOCKMAP cell list. */
    private static List<Integer> blockmapCell(ByteBuffer input, int dataLength, int offset, int index) {
        int byteOffset = offset * Short.BYTES;
        if (byteOffset > dataLength - 2 * Short.BYTES) {
            throw new DecodeFailure(
                    DoomDiagnosticCode.MAP_BLOCKMAP_INVALID,
                    "BLOCKMAP/cells/" + index,
                    Map.of("offset", Integer.toString(offset)));
        }
        ByteBuffer cell = input.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        cell.position(byteOffset);
        cell.getShort();
        List<Integer> linedefs = new ArrayList<>();
        while (cell.remaining() >= Short.BYTES) {
            int value = unsigned(cell);
            if (value == 0xffff) {
                return List.copyOf(linedefs);
            }
            linedefs.add(value);
        }
        throw new DecodeFailure(
                DoomDiagnosticCode.MAP_BLOCKMAP_INVALID, "BLOCKMAP/cells/" + index, Map.of("reason", "unterminated"));
    }

    /** Returns aligned little-endian input for one fixed-size record table. */
    private static ByteBuffer records(byte[] data, int recordSize, String name) {
        if (data.length % recordSize != 0) {
            throw new DecodeFailure(
                    DoomDiagnosticCode.MAP_RECORD_SIZE_INVALID,
                    name,
                    Map.of("recordSize", Integer.toString(recordSize), "size", Integer.toString(data.length)));
        }
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    /** Reads one unsigned little-endian short. */
    private static int unsigned(ByteBuffer input) {
        return Short.toUnsignedInt(input.getShort());
    }

    /** Reads an unsigned table index using {@code -1} for the absent sentinel. */
    private static int indexOrMissing(ByteBuffer input) {
        int value = unsigned(input);
        return value == 0xffff ? -1 : value;
    }

    /** Reads one NUL-padded uppercase ASCII Doom name. */
    private static String name(ByteBuffer input) {
        byte[] bytes = new byte[8];
        input.get(bytes);
        int length = 0;
        while (length < bytes.length && bytes[length] != 0) {
            length++;
        }
        return new String(bytes, 0, length, StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
    }

    /** Reads one BSP bounding box in source field order. */
    private static DoomMap.BoundingBox boundingBox(ByteBuffer input) {
        return new DoomMap.BoundingBox(input.getShort(), input.getShort(), input.getShort(), input.getShort());
    }

    /** Reads one BSP child reference and separates its subsector flag. */
    private static DoomMap.NodeChild child(ByteBuffer input) {
        int value = unsigned(input);
        return new DoomMap.NodeChild((value & 0x8000) != 0, value & 0x7fff);
    }

    /** Converts raw bytes to immutable unsigned integer values. */
    private static List<Integer> unsignedBytes(byte[] data) {
        List<Integer> result = new ArrayList<>(data.length);
        for (byte value : data) {
            result.add(Byte.toUnsignedInt(value));
        }
        return List.copyOf(result);
    }

    /** Validates every linedef table reference. */
    private static void validateLinedefs(List<DoomMap.Linedef> linedefs, int vertexCount, int sidedefCount) {
        for (int index = 0; index < linedefs.size(); index++) {
            DoomMap.Linedef linedef = linedefs.get(index);
            requireIndex(linedef.startVertex(), vertexCount, "LINEDEFS/" + index + "/startVertex");
            requireIndex(linedef.endVertex(), vertexCount, "LINEDEFS/" + index + "/endVertex");
            requireIndex(linedef.rightSidedef(), sidedefCount, "LINEDEFS/" + index + "/rightSidedef");
            if (linedef.leftSidedef() != -1) {
                requireIndex(linedef.leftSidedef(), sidedefCount, "LINEDEFS/" + index + "/leftSidedef");
            }
        }
    }

    /** Validates every sidedef sector reference. */
    private static void validateSidedefs(List<DoomMap.Sidedef> sidedefs, int sectorCount) {
        for (int index = 0; index < sidedefs.size(); index++) {
            requireIndex(sidedefs.get(index).sector(), sectorCount, "SIDEDEFS/" + index + "/sector");
        }
    }

    /** Validates every parsed blockmap linedef reference. */
    private static void validateBlockmap(DoomMap.Blockmap blockmap, int linedefCount) {
        for (int cellIndex = 0; cellIndex < blockmap.cells().size(); cellIndex++) {
            List<Integer> cell = blockmap.cells().get(cellIndex);
            for (int entryIndex = 0; entryIndex < cell.size(); entryIndex++) {
                requireIndex(cell.get(entryIndex), linedefCount, "BLOCKMAP/cells/" + cellIndex + "/" + entryIndex);
            }
        }
    }

    /** Validates that REJECT contains one bit per ordered sector pair. */
    private static void validateReject(List<Integer> rejectBytes, int sectorCount) {
        long bits = (long) sectorCount * sectorCount;
        long requiredBytes = (bits + Byte.SIZE - 1) / Byte.SIZE;
        if (rejectBytes.size() < requiredBytes) {
            throw new DecodeFailure(
                    DoomDiagnosticCode.MAP_REJECT_SIZE_INVALID,
                    "REJECT",
                    Map.of(
                            "actualBytes", Integer.toString(rejectBytes.size()),
                            "requiredBytes", Long.toString(requiredBytes)));
        }
    }

    /** Validates every seg table reference and direction. */
    private static void validateSegs(List<DoomMap.Seg> segs, int vertexCount, int linedefCount) {
        for (int index = 0; index < segs.size(); index++) {
            DoomMap.Seg seg = segs.get(index);
            requireIndex(seg.startVertex(), vertexCount, "SEGS/" + index + "/startVertex");
            requireIndex(seg.endVertex(), vertexCount, "SEGS/" + index + "/endVertex");
            requireIndex(seg.linedef(), linedefCount, "SEGS/" + index + "/linedef");
            if (seg.direction() > 1) {
                throw new DecodeFailure(
                        DoomDiagnosticCode.MAP_VALUE_INVALID,
                        "SEGS/" + index + "/direction",
                        Map.of("value", Integer.toString(seg.direction())));
            }
        }
    }

    /** Validates each subsector's contiguous seg range. */
    private static void validateSubsectors(List<DoomMap.Subsector> subsectors, int segCount) {
        for (int index = 0; index < subsectors.size(); index++) {
            DoomMap.Subsector subsector = subsectors.get(index);
            if (subsector.firstSeg() > segCount || subsector.segCount() > segCount - subsector.firstSeg()) {
                throw new DecodeFailure(
                        DoomDiagnosticCode.MAP_REFERENCE_INVALID,
                        "SSECTORS/" + index + "/segs",
                        Map.of(
                                "firstSeg", Integer.toString(subsector.firstSeg()),
                                "segCount", Integer.toString(subsector.segCount()),
                                "tableSize", Integer.toString(segCount)));
            }
        }
    }

    /** Validates both child references of every BSP node. */
    private static void validateNodes(List<DoomMap.Node> nodes, int subsectorCount) {
        for (int index = 0; index < nodes.size(); index++) {
            DoomMap.Node node = nodes.get(index);
            validateNodeChild(node.right().child(), nodes.size(), subsectorCount, "NODES/" + index + "/rightChild");
            validateNodeChild(node.left().child(), nodes.size(), subsectorCount, "NODES/" + index + "/leftChild");
        }
    }

    /** Validates one node-or-subsector reference against the selected table. */
    private static void validateNodeChild(DoomMap.NodeChild child, int nodeCount, int subsectorCount, String location) {
        int size = child.subsector() ? subsectorCount : nodeCount;
        requireIndex(child.index(), size, location);
    }

    /** Requires an index within a zero-based table. */
    private static void requireIndex(int value, int size, String location) {
        if (value < 0 || value >= size) {
            throw new DecodeFailure(
                    DoomDiagnosticCode.MAP_REFERENCE_INVALID,
                    location,
                    Map.of("index", Integer.toString(value), "tableSize", Integer.toString(size)));
        }
    }

    /** Reports whether a WAD name is a conventional Doom map marker. */
    private static boolean isMapMarker(String name) {
        return name.length() == 5
                        && name.startsWith("MAP")
                        && isAsciiDigit(name.charAt(3))
                        && isAsciiDigit(name.charAt(4))
                || name.length() == 4
                        && name.charAt(0) == 'E'
                        && isAsciiDigit(name.charAt(1))
                        && name.charAt(2) == 'M'
                        && isAsciiDigit(name.charAt(3));
    }

    /** Reports whether one character is an ASCII decimal digit. */
    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }

    /** Creates one source-aware content error. */
    private static DoomDiagnostic error(
            WadArchive archive, DoomDiagnosticCode code, String location, Map<String, String> details) {
        return new DoomDiagnostic(
                DoomDiagnostic.Severity.ERROR, code, archive.provenance().source(), location, details);
    }

    /** Returns stable non-null technical failure detail. */
    private static String failureMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message;
    }

    /** Internal typed failure retaining localization-ready diagnostic details. */
    private static final class DecodeFailure extends RuntimeException {
        private final DoomDiagnosticCode code;
        private final String location;
        private final Map<String, String> details;

        /** Stores one implementation failure before source context is attached. */
        private DecodeFailure(DoomDiagnosticCode code, String location, Map<String, String> details) {
            super(code.code() + " at " + location);
            this.code = Objects.requireNonNull(code, "code");
            this.location = Objects.requireNonNull(location, "location");
            this.details = Map.copyOf(details);
        }

        /** Returns the feature-owned code. */
        private DoomDiagnosticCode code() {
            return code;
        }

        /** Returns the location relative to one map. */
        private String location() {
            return location;
        }

        /** Returns language-neutral failure values. */
        private Map<String, String> details() {
            return details;
        }
    }
}
