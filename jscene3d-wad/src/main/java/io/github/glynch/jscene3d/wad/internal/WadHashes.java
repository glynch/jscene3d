/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Complete-file fingerprinting policy for WAD source provenance. */
public final class WadHashes {
    /** Prevents instantiation of this hashing policy. */
    private WadHashes() {
        throw new AssertionError("WadHashes cannot be instantiated");
    }

    /**
     * Returns the lowercase SHA-256 fingerprint of one file.
     *
     * @param source file to fingerprint
     * @return lowercase hexadecimal SHA-256 fingerprint
     * @throws IOException when the complete source cannot be read
     */
    public static String sha256(Path source) throws IOException {
        MessageDigest digest = newDigest();
        try (DigestInputStream input = new DigestInputStream(Files.newInputStream(source), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Creates the SHA-256 algorithm required by every supported Java runtime. */
    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("Java runtime does not provide SHA-256", exception);
        }
    }
}
