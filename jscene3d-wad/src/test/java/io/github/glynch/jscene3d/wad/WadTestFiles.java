/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Creates small deterministic WAD fixtures without introducing a production writer. */
final class WadTestFiles {
    /** Prevents instantiation of this fixture factory. */
    private WadTestFiles() {
        throw new AssertionError("WadTestFiles cannot be instantiated");
    }

    /** Writes a structurally valid archive with the supplied ordered lumps. */
    static Path write(Path target, String signature, List<TestLump> lumps) throws IOException {
        int contentSize = lumps.stream().mapToInt(item -> item.content.length).sum();
        int directoryOffset = 12 + contentSize;
        ByteBuffer bytes =
                ByteBuffer.allocate(directoryOffset + lumps.size() * 16).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(signature.getBytes(StandardCharsets.US_ASCII));
        bytes.putInt(lumps.size());
        bytes.putInt(directoryOffset);
        lumps.forEach(lump -> bytes.put(lump.content));
        int offset = 12;
        for (TestLump lump : lumps) {
            bytes.putInt(offset);
            bytes.putInt(lump.content.length);
            putName(bytes, lump.name);
            offset += lump.content.length;
        }
        Files.write(target, bytes.array());
        return target;
    }

    /** Writes an archive header followed by caller-supplied directory bytes. */
    static Path writeRaw(Path target, String signature, int lumpCount, int directoryOffset, byte[] remainder)
            throws IOException {
        ByteBuffer bytes = ByteBuffer.allocate(12 + remainder.length).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(signature.getBytes(StandardCharsets.US_ASCII));
        bytes.putInt(lumpCount);
        bytes.putInt(directoryOffset);
        bytes.put(remainder);
        Files.write(target, bytes.array());
        return target;
    }

    /** Writes one fixed-width NUL-padded directory name. */
    static void putName(ByteBuffer target, String name) {
        byte[] encoded = name.getBytes(StandardCharsets.US_ASCII);
        if (encoded.length > 8) {
            throw new IllegalArgumentException("test lump name is longer than eight bytes");
        }
        target.put(encoded);
        target.put(new byte[8 - encoded.length]);
    }

    /** Opaque name and content used while constructing a fixture. */
    static final class TestLump {
        private final String name;
        private final byte[] content;

        /** Stores fixture content. */
        TestLump(String name, byte[] content) {
            this.name = name;
            this.content = content.clone();
        }
    }
}
