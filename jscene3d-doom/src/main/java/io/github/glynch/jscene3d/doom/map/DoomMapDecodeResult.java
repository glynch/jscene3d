/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.map;

import io.github.glynch.jscene3d.doom.diagnostic.DoomDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of decoding one classic Doom map from a validated WAD.
 *
 * @param map decoded map when no error prevented decoding
 * @param diagnostics immutable ordered diagnostics
 */
public record DoomMapDecodeResult(Optional<DoomMap> map, List<DoomDiagnostic> diagnostics) {
    /** Creates an immutable decode result. */
    public DoomMapDecodeResult {
        Objects.requireNonNull(map, "map");
        diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Returns whether decoding produced a map without errors.
     *
     * @return {@code true} exactly when a usable map is present
     */
    public boolean isValid() {
        return map.isPresent()
                && diagnostics.stream().noneMatch(item -> item.severity() == DoomDiagnostic.Severity.ERROR);
    }
}
