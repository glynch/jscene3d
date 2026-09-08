/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.geometry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of constructing renderer-independent geometry and its ordered diagnostics.
 *
 * @param geometry constructed geometry, or empty when construction failed
 * @param diagnostics ordered construction diagnostics
 */
public record DoomGeometryBuildResult(Optional<DoomStaticGeometry> geometry, List<DoomGeometryDiagnostic> diagnostics) {
    /** Creates an immutable result. */
    public DoomGeometryBuildResult {
        Objects.requireNonNull(geometry, "geometry");
        diagnostics = List.copyOf(diagnostics);
        if (geometry.isPresent()
                && diagnostics.stream().anyMatch(item -> item.severity() == DoomGeometryDiagnostic.Severity.ERROR)) {
            throw new IllegalArgumentException("geometry cannot accompany error diagnostics");
        }
    }

    /**
     * Returns whether construction produced usable geometry.
     *
     * @return {@code true} when geometry is present
     */
    public boolean isValid() {
        return geometry.isPresent();
    }
}
