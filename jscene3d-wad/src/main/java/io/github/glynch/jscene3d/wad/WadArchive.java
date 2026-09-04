/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.wad.internal.BoundedFileInputStream;
import io.github.glynch.jscene3d.wad.internal.Preconditions;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable validated directory for one WAD source.
 *
 * <p>The archive owns no open file handle. Each call to {@link #openStream(WadLump)} opens an independent bounded stream,
 * and the caller must close that stream. Source provenance describes the bytes observed during loading; later source
 * replacement or size changes cause reads to fail rather than escape the validated lump bounds.
 */
public final class WadArchive {
    private final WadProvenance provenance;
    private final WadKind kind;
    private final List<WadLump> lumps;

    /**
     * Creates a validated archive description.
     *
     * @param provenance source-byte provenance
     * @param kind header-declared archive kind
     * @param lumps directory entries in source order
     */
    public WadArchive(WadProvenance provenance, WadKind kind, List<WadLump> lumps) {
        this.provenance = Objects.requireNonNull(provenance, "provenance");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.lumps = validateLumps(lumps, provenance.fileSize());
    }

    /**
     * Returns source-byte provenance captured during loading.
     *
     * @return immutable source provenance
     */
    public WadProvenance provenance() {
        return provenance;
    }

    /**
     * Returns the archive kind declared in the header.
     *
     * @return header-declared kind
     */
    public WadKind kind() {
        return kind;
    }

    /**
     * Returns every opaque lump in directory order, including duplicate names.
     *
     * @return immutable ordered lumps
     */
    public List<WadLump> lumps() {
        return lumps;
    }

    /**
     * Returns all lumps with one case-insensitive WAD name in directory order.
     *
     * @param name printable ASCII WAD name of at most eight characters
     * @return immutable matching lumps
     */
    public List<WadLump> lumpsNamed(String name) {
        String normalizedName = Preconditions.requireLumpName(name, "name");
        return lumps.stream().filter(lump -> lump.name().equals(normalizedName)).toList();
    }

    /**
     * Returns the last lump with one case-insensitive name.
     *
     * @param name printable ASCII WAD name of at most eight characters
     * @return last matching lump when present
     */
    public Optional<WadLump> lastLumpNamed(String name) {
        String normalizedName = Preconditions.requireLumpName(name, "name");
        for (int index = lumps.size() - 1; index >= 0; index--) {
            WadLump lump = lumps.get(index);
            if (lump.name().equals(normalizedName)) {
                return Optional.of(lump);
            }
        }
        return Optional.empty();
    }

    /**
     * Opens an independent stream bounded to exactly one validated lump.
     *
     * @param lump directory entry belonging to this archive
     * @return caller-owned bounded stream
     * @throws IOException when the source cannot be opened or no longer has its validated size
     */
    public InputStream openStream(WadLump lump) throws IOException {
        WadLump owned = requireOwned(lump);
        return BoundedFileInputStream.openStream(
                provenance.source(), owned.offset(), owned.size(), provenance.fileSize());
    }

    /**
     * Reads one lump after applying a caller-selected allocation limit.
     *
     * @param lump directory entry belonging to this archive
     * @param maximumBytes largest accepted allocation
     * @return newly allocated lump bytes
     * @throws IOException when the source cannot be read completely
     */
    public byte[] readAllBytes(WadLump lump, int maximumBytes) throws IOException {
        WadLump owned = requireOwned(lump);
        int validMaximum = Preconditions.requireNonNegative(maximumBytes, "maximumBytes");
        if (owned.size() > validMaximum) {
            throw new IllegalArgumentException("lump size " + owned.size() + " exceeds maximumBytes " + validMaximum);
        }
        try (InputStream input = openStream(owned)) {
            byte[] content = input.readNBytes(owned.size());
            if (content.length != owned.size()) {
                throw new EOFException("WAD source ended before the validated lump was complete");
            }
            return content;
        }
    }

    /** Returns an owned lump or rejects metadata from another archive. */
    private WadLump requireOwned(WadLump lump) {
        WadLump supplied = Objects.requireNonNull(lump, "lump");
        if (supplied.index() >= lumps.size() || !lumps.get(supplied.index()).equals(supplied)) {
            throw new IllegalArgumentException("lump does not belong to this archive");
        }
        return supplied;
    }

    /** Copies and validates sequential directory metadata against source bounds. */
    private static List<WadLump> validateLumps(List<WadLump> lumps, long fileSize) {
        List<WadLump> copied = List.copyOf(lumps);
        for (int index = 0; index < copied.size(); index++) {
            WadLump lump = copied.get(index);
            if (lump.index() != index) {
                throw new IllegalArgumentException("lump index does not match directory position: " + lump.index());
            }
            if (lump.offset() > fileSize || lump.size() > fileSize - lump.offset()) {
                throw new IllegalArgumentException("lump extends beyond the source file: " + lump.name());
            }
        }
        return copied;
    }
}
