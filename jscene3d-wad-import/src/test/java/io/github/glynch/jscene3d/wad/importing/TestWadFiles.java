/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.importing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes small deterministic WAD fixtures owned by a JUnit temporary directory. */
final class TestWadFiles {
    /** Prevents construction of this test fixture writer. */
    private TestWadFiles() {
        throw new AssertionError("TestWadFiles cannot be instantiated");
    }

    /** Writes one IWAD whose data and directory contain the supplied ordered lumps. */
    static void write(Path path, List<LumpContent> lumps) throws IOException {
        int contentSize = lumps.stream().mapToInt(lump -> lump.content().length).sum();
        int directoryOffset = 12 + contentSize;
        ByteBuffer buffer =
                ByteBuffer.allocate(directoryOffset + lumps.size() * 16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("IWAD".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(lumps.size());
        buffer.putInt(directoryOffset);
        for (LumpContent lump : lumps) {
            buffer.put(lump.content());
        }
        int offset = 12;
        for (LumpContent lump : lumps) {
            buffer.putInt(offset);
            buffer.putInt(lump.content().length);
            putName(buffer, lump.name());
            offset += lump.content().length;
        }
        Files.write(path, buffer.array());
    }

    /** Writes one NUL-padded fixed-width WAD name. */
    private static void putName(ByteBuffer buffer, String name) {
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        buffer.put(bytes);
        for (int index = bytes.length; index < 8; index++) {
            buffer.put((byte) 0);
        }
    }

    /** One ordered test lump and its deliberately copied opaque bytes. */
    static final class LumpContent {
        private final String name;
        private final byte[] content;

        /** Stores one test lump after copying its bytes. */
        LumpContent(String name, byte[] content) {
            this.name = name;
            this.content = content.clone();
        }

        /** Returns the printable WAD name. */
        String name() {
            return name;
        }

        /** Returns a fresh copy of the opaque content. */
        byte[] content() {
            return content.clone();
        }
    }
}
