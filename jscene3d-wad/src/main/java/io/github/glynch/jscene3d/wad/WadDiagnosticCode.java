/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.wad.internal.Preconditions;

/** Stable diagnostic codes and English fallback messages owned by the WAD feature. */
public enum WadDiagnosticCode implements DiagnosticCode {
    /** The source path does not identify a regular file. */
    SOURCE_MISSING("wad.source.missing", "WAD source is not a regular file"),

    /** The complete source fingerprint differs from the expected fingerprint. */
    SOURCE_FINGERPRINT_MISMATCH("wad.source.sha256", "The WAD fingerprint does not match the expected fingerprint"),

    /** The source could not be read. */
    SOURCE_READ_FAILED("wad.source.read", "The WAD source could not be read"),

    /** The source size changed during validation. */
    SOURCE_CHANGED("wad.source.changed", "The WAD source changed while it was being loaded"),

    /** The source ends before the complete fixed header. */
    HEADER_TRUNCATED("wad.header.truncated", "The WAD header is incomplete"),

    /** The header signature is neither IWAD nor PWAD. */
    HEADER_SIGNATURE_INVALID("wad.header.signature", "The WAD signature must be IWAD or PWAD"),

    /** The declared directory entry count is unsupported. */
    DIRECTORY_COUNT_INVALID("wad.directory.count", "The WAD lump count is outside the supported range"),

    /** The declared directory offset is negative. */
    DIRECTORY_OFFSET_INVALID("wad.directory.offset", "The WAD directory offset must not be negative"),

    /** The declared directory extends outside the source. */
    DIRECTORY_OUT_OF_BOUNDS("wad.directory.bounds", "The WAD directory extends beyond the source file"),

    /** A directory entry identifies bytes outside the source. */
    LUMP_OUT_OF_BOUNDS("wad.lump.bounds", "A WAD lump extends beyond the source file"),

    /** A directory entry contains an invalid fixed-width name. */
    LUMP_NAME_INVALID("wad.lump.name", "A WAD lump name must contain printable ASCII followed only by NUL padding");

    private final String value;
    private final String message;

    /** Stores one stable code and its English fallback. */
    WadDiagnosticCode(String value, String message) {
        this.value = Preconditions.requireNonBlank(value, "value");
        this.message = Preconditions.requireNonBlank(message, "message");
    }

    @Override
    public String code() {
        return value;
    }

    @Override
    public String defaultMessage() {
        return message;
    }
}
