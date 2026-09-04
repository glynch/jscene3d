/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.examples;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Creates the minimal valid classic Doom map used by the feature example. */
final class ExampleDoomWad {
    /** Prevents construction of this stateless fixture writer. */
    private ExampleDoomWad() {
        throw new AssertionError("ExampleDoomWad cannot be instantiated");
    }

    /** Writes a PWAD containing one complete MAP01. */
    static Path write(Path target) throws IOException {
        List<LumpContent> lumps = List.of(
                new LumpContent("MAP01", new byte[0]),
                new LumpContent("THINGS", shorts(64, -32, 90, 1, 7)),
                new LumpContent("LINEDEFS", shorts(0, 1, 1, 0, 0, 0, 0xffff)),
                new LumpContent("SIDEDEFS", sidedef(8, -4, "UPPER", "-", "MIDDLE", 0)),
                new LumpContent("VERTEXES", shorts(0, 0, 128, 0)),
                new LumpContent("SEGS", shorts(0, 1, 0, 0, 0, 0)),
                new LumpContent("SSECTORS", shorts(1, 0)),
                new LumpContent("NODES", new byte[0]),
                new LumpContent("SECTORS", sector(0, 128, "FLOOR0_1", "CEIL1_1", 160, 0, 0)),
                new LumpContent("REJECT", new byte[] {0}),
                new LumpContent("BLOCKMAP", shorts(0, 0, 1, 1, 5, 0, 0, 0xffff)));
        writeArchive(target, lumps);
        return target;
    }

    /** Writes one ordered PWAD directory and content area. */
    private static void writeArchive(Path target, List<LumpContent> lumps) throws IOException {
        int contentSize = lumps.stream().mapToInt(lump -> lump.content().length).sum();
        int directoryOffset = 12 + contentSize;
        ByteBuffer bytes =
                ByteBuffer.allocate(directoryOffset + lumps.size() * 16).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put("PWAD".getBytes(StandardCharsets.US_ASCII));
        bytes.putInt(lumps.size());
        bytes.putInt(directoryOffset);
        for (LumpContent lump : lumps) {
            bytes.put(lump.content());
        }
        int offset = 12;
        for (LumpContent lump : lumps) {
            bytes.putInt(offset);
            bytes.putInt(lump.content().length);
            putName(bytes, lump.name());
            offset += lump.content().length;
        }
        Files.write(target, bytes.array());
    }

    /** Encodes little-endian sixteen-bit values. */
    private static byte[] shorts(int... values) {
        ByteBuffer bytes = ByteBuffer.allocate(values.length * Short.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int value : values) {
            bytes.putShort((short) value);
        }
        return bytes.array();
    }

    /** Encodes one classic sidedef. */
    private static byte[] sidedef(int xOffset, int yOffset, String upper, String lower, String middle, int sector) {
        ByteBuffer bytes = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putShort((short) xOffset);
        bytes.putShort((short) yOffset);
        putName(bytes, upper);
        putName(bytes, lower);
        putName(bytes, middle);
        bytes.putShort((short) sector);
        return bytes.array();
    }

    /** Encodes one classic sector. */
    private static byte[] sector(
            int floor, int ceiling, String floorTexture, String ceilingTexture, int light, int special, int tag) {
        ByteBuffer bytes = ByteBuffer.allocate(26).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putShort((short) floor);
        bytes.putShort((short) ceiling);
        putName(bytes, floorTexture);
        putName(bytes, ceilingTexture);
        bytes.putShort((short) light);
        bytes.putShort((short) special);
        bytes.putShort((short) tag);
        return bytes.array();
    }

    /** Writes one NUL-padded eight-byte WAD name. */
    private static void putName(ByteBuffer target, String name) {
        byte[] encoded = name.getBytes(StandardCharsets.US_ASCII);
        target.put(encoded);
        target.put(new byte[8 - encoded.length]);
    }

    /** Test-sized immutable lump data owned by this example. */
    private static final class LumpContent {
        private final String name;
        private final byte[] content;

        /** Stores one example lump with private content ownership. */
        private LumpContent(String name, byte[] content) {
            this.name = Objects.requireNonNull(name, "name");
            this.content = Objects.requireNonNull(content, "content").clone();
        }

        /** Returns the fixed-width source name. */
        private String name() {
            return name;
        }

        /** Returns a private content copy. */
        private byte[] content() {
            return content.clone();
        }
    }
}
