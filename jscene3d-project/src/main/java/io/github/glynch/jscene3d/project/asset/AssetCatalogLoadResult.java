/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of scanning one project asset tree. */
public final class AssetCatalogLoadResult {
    private final Optional<AssetCatalog> catalog;
    private final List<ProjectDiagnostic> diagnostics;

    /** Stores a catalog exactly when no error diagnostics were produced. */
    AssetCatalogLoadResult(Optional<AssetCatalog> catalog, List<ProjectDiagnostic> diagnostics) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.diagnostics = List.copyOf(diagnostics);
        boolean hasErrors = this.diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (catalog.isPresent() == hasErrors) {
            throw new IllegalArgumentException("a catalog must be present exactly when diagnostics contain no errors");
        }
    }

    /**
     * Returns the immutable catalog when scanning succeeded.
     *
     * @return validated asset catalog
     */
    public Optional<AssetCatalog> catalog() {
        return catalog;
    }

    /**
     * Returns ordered scan diagnostics.
     *
     * @return immutable diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns whether a catalog is available.
     *
     * @return {@code true} when scanning succeeded
     */
    public boolean isValid() {
        return catalog.isPresent();
    }
}
