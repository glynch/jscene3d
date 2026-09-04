/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.examples;

import io.github.glynch.jscene3d.wad.WadKind;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates bounded WAD sources used by self-contained examples. */
final class ExampleWadFiles {
    /** Prevents construction of this stateless fixture writer. */
    private ExampleWadFiles() {
        throw new AssertionError("ExampleWadFiles cannot be instantiated");
    }

    /** Writes one small archive containing a single opaque lump. */
    static Path writeSingleLump(Path target, WadKind kind, String name, String content) throws IOException {
        byte[] encodedContent = content.getBytes(StandardCharsets.US_ASCII);
        int directoryOffset = 12 + encodedContent.length;
        ByteBuffer bytes = ByteBuffer.allocate(directoryOffset + 16).order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(kind.name().getBytes(StandardCharsets.US_ASCII));
        bytes.putInt(1);
        bytes.putInt(directoryOffset);
        bytes.put(encodedContent);
        bytes.putInt(12);
        bytes.putInt(encodedContent.length);
        byte[] encodedName = name.getBytes(StandardCharsets.US_ASCII);
        bytes.put(encodedName);
        bytes.put(new byte[8 - encodedName.length]);
        Files.write(target, bytes.array());
        return target;
    }
}
