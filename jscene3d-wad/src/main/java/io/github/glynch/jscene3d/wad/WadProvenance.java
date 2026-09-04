/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.wad.internal.Preconditions;
import java.nio.file.Path;

/**
 * Immutable identity of the source bytes from which an archive directory was decoded.
 *
 * @param source normalized absolute source path
 * @param fileSize source size at load time
 * @param sha256 lowercase SHA-256 of the complete source at load time
 */
public record WadProvenance(Path source, long fileSize, String sha256) {
    /** Creates validated source provenance. */
    public WadProvenance {
        source = Preconditions.requireAbsoluteNormalized(source, "source");
        fileSize = Preconditions.requireNonNegative(fileSize, "fileSize");
        sha256 = Preconditions.requireSha256(sha256, "sha256");
    }
}
