/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable SHA-256 fingerprint of exact persisted authored-source bytes. */
public final class SourceFingerprint {
    private static final String ALGORITHM = "SHA-256";

    private final String hexadecimal;

    /** Stores one already encoded SHA-256 value. */
    private SourceFingerprint(String hexadecimal) {
        this.hexadecimal = hexadecimal;
    }

    /**
     * Fingerprints exact source bytes without retaining or exposing a mutable byte array.
     *
     * @param content exact persisted source bytes
     * @return immutable SHA-256 fingerprint
     */
    public static SourceFingerprint sha256(byte[] content) {
        Objects.requireNonNull(content, "content");
        try {
            byte[] digest = MessageDigest.getInstance(ALGORITHM).digest(content);
            return new SourceFingerprint(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("The Java runtime must provide SHA-256", exception);
        }
    }

    /**
     * Returns the lower-case hexadecimal SHA-256 value.
     *
     * @return immutable hexadecimal digest
     */
    public String hexadecimal() {
        return hexadecimal;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof SourceFingerprint fingerprint && hexadecimal.equals(fingerprint.hexadecimal);
    }

    @Override
    public int hashCode() {
        return hexadecimal.hashCode();
    }

    @Override
    public String toString() {
        return "sha256:" + hexadecimal;
    }
}
