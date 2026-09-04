/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.internal;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Strict decoding policy for fixed-width WAD directory names. */
public final class WadNames {
    /** Prevents instantiation of this name-decoding policy. */
    private WadNames() {
        throw new AssertionError("WadNames cannot be instantiated");
    }

    /**
     * Returns one normalized name when bytes are printable ASCII followed only by NUL padding.
     *
     * @param rawName fixed-width directory name bytes
     * @return normalized name, or empty when the encoding is invalid
     */
    public static Optional<String> decode(byte[] rawName) {
        int length = 0;
        boolean padded = false;
        for (byte value : rawName) {
            int character = Byte.toUnsignedInt(value);
            if (character == 0) {
                padded = true;
            } else if (padded || character < 0x20 || character > 0x7e) {
                return Optional.empty();
            } else {
                length++;
            }
        }
        String decoded = new String(rawName, 0, length, StandardCharsets.US_ASCII);
        return Optional.of(Preconditions.requireLumpName(decoded, "decoded name"));
    }
}
