/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.wad.internal.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * One lump resolved together with its owning archive and explicit layer position.
 *
 * @param layer zero-based archive-layer index
 * @param archive owning archive
 * @param lump archive directory entry
 */
public record WadLumpReference(int layer, WadArchive archive, WadLump lump) {
    /** Creates a validated reference to an owned archive entry. */
    public WadLumpReference {
        layer = Preconditions.requireNonNegative(layer, "layer");
        Objects.requireNonNull(archive, "archive");
        Objects.requireNonNull(lump, "lump");
        if (lump.index() >= archive.lumps().size()
                || !archive.lumps().get(lump.index()).equals(lump)) {
            throw new IllegalArgumentException("lump does not belong to archive");
        }
    }

    /**
     * Opens an independent stream bounded to this lump.
     *
     * @return caller-owned bounded stream
     * @throws IOException when the source cannot be opened or changed size
     */
    public InputStream openStream() throws IOException {
        return archive.openStream(lump);
    }

    /**
     * Reads this lump after applying a caller-selected allocation limit.
     *
     * @param maximumBytes largest accepted allocation
     * @return newly allocated lump bytes
     * @throws IOException when the source cannot be read completely
     */
    public byte[] readAllBytes(int maximumBytes) throws IOException {
        return archive.readAllBytes(lump, maximumBytes);
    }
}
