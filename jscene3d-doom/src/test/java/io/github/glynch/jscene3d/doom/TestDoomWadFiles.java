/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom;

import io.github.glynch.jscene3d.wad.WadArchive;
import io.github.glynch.jscene3d.wad.WadLoader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Creates deterministic classic Doom WAD fixtures for public-interface tests. */
final class TestDoomWadFiles {
    /** Prevents construction of this stateless fixture writer. */
    private TestDoomWadFiles() {
        throw new AssertionError("TestDoomWadFiles cannot be instantiated");
    }

    /** Returns a complete minimal classic map sequence without its marker. */
    static List<LumpContent> validMapLumps() {
        return new ArrayList<>(List.of(
                new LumpContent("THINGS", shorts(64, -32, 90, 1, 7)),
                new LumpContent("LINEDEFS", shorts(0, 1, 1, 0, 0, 0, 0xffff)),
                new LumpContent("SIDEDEFS", sidedef(8, -4, "UPPER", "-", "MIDDLE", 0)),
                new LumpContent("VERTEXES", shorts(0, 0, 128, 0)),
                new LumpContent("SEGS", shorts(0, 1, 0, 0, 0, 0)),
                new LumpContent("SSECTORS", shorts(1, 0)),
                new LumpContent("NODES", new byte[0]),
                new LumpContent("SECTORS", sector(0, 128, "FLOOR0_1", "CEIL1_1", 160, 0, 0)),
                new LumpContent("REJECT", new byte[] {0}),
                new LumpContent("BLOCKMAP", shorts(0, 0, 1, 1, 5, 0, 0, 0xffff))));
    }

    /** Returns a marker followed by the complete minimal classic map sequence. */
    static List<LumpContent> validMap(String name) {
        List<LumpContent> lumps = new ArrayList<>();
        lumps.add(new LumpContent(name, new byte[0]));
        lumps.addAll(validMapLumps());
        return lumps;
    }

    /** Replaces one named lump in a complete minimal MAP01 sequence. */
    static List<LumpContent> validMapReplacing(String name, byte[] content) {
        List<LumpContent> lumps = validMap("MAP01");
        for (int index = 0; index < lumps.size(); index++) {
            if (lumps.get(index).name().equals(name)) {
                lumps.set(index, new LumpContent(name, content));
                return lumps;
            }
        }
        throw new IllegalArgumentException("unknown classic map lump: " + name);
    }

    /** Writes and loads one valid PWAD containing the supplied ordered lumps. */
    static WadArchive writeAndLoad(Path source, List<LumpContent> lumps) throws IOException {
        write(source, lumps);
        return WadLoader.load(source).archive().orElseThrow();
    }

    /** Writes one PWAD containing the supplied ordered lumps. */
    static Path write(Path source, List<LumpContent> lumps) throws IOException {
        List<LumpContent> copied = List.copyOf(lumps);
        int contentSize =
                copied.stream().mapToInt(lump -> lump.content().length).sum();
        int directoryOffset = 12 + contentSize;
        ByteBuffer bytes =
                ByteBuffer.allocate(directoryOffset + copied.size() * 16).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put("PWAD".getBytes(StandardCharsets.US_ASCII));
        bytes.putInt(copied.size());
        bytes.putInt(directoryOffset);
        for (LumpContent lump : copied) {
            bytes.put(lump.content());
        }
        int offset = 12;
        for (LumpContent lump : copied) {
            bytes.putInt(offset);
            bytes.putInt(lump.content().length);
            putName(bytes, lump.name());
            offset += lump.content().length;
        }
        Files.write(source, bytes.array());
        return source;
    }

    /** Encodes little-endian sixteen-bit values. */
    static byte[] shorts(int... values) {
        ByteBuffer bytes = ByteBuffer.allocate(values.length * Short.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int value : values) {
            bytes.putShort((short) value);
        }
        return bytes.array();
    }

    /** Encodes one classic sidedef record. */
    static byte[] sidedef(int xOffset, int yOffset, String upper, String lower, String middle, int sector) {
        ByteBuffer bytes = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putShort((short) xOffset);
        bytes.putShort((short) yOffset);
        putName(bytes, upper);
        putName(bytes, lower);
        putName(bytes, middle);
        bytes.putShort((short) sector);
        return bytes.array();
    }

    /** Encodes one classic sector record. */
    static byte[] sector(
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

    /** Immutable test lump content with defensive byte-array ownership. */
    static final class LumpContent {
        private final String name;
        private final byte[] content;

        /** Stores one test lump. */
        LumpContent(String name, byte[] content) {
            this.name = Objects.requireNonNull(name, "name");
            this.content = Objects.requireNonNull(content, "content").clone();
        }

        /** Returns the lump name. */
        String name() {
            return name;
        }

        /** Returns a defensive content copy. */
        byte[] content() {
            return content.clone();
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                    || other instanceof LumpContent lump
                            && name.equals(lump.name)
                            && Arrays.equals(content, lump.content);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + Arrays.hashCode(content);
        }

        @Override
        public String toString() {
            return "LumpContent[name=" + name + ", content=" + Arrays.toString(content) + ']';
        }
    }
}
