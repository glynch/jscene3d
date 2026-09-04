/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable outcome of validating and indexing one WAD source.
 *
 * @param archive validated archive when loading succeeded
 * @param diagnostics ordered source diagnostics
 */
public record WadLoadResult(Optional<WadArchive> archive, List<WadDiagnostic> diagnostics) {
    /** Creates an immutable load outcome. */
    public WadLoadResult {
        Objects.requireNonNull(archive, "archive");
        diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Reports whether an archive was produced without error diagnostics.
     *
     * @return {@code true} when callers may use the archive
     */
    public boolean isValid() {
        return archive.isPresent()
                && diagnostics.stream().noneMatch(item -> item.severity() == WadDiagnostic.Severity.ERROR);
    }
}
