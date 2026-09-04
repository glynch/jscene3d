/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;

/** Immutable result of descriptor discovery and registered-type catalog construction. */
public final class ExtensionCatalogLoadResult {
    private final RegisteredTypeCatalog catalog;
    private final List<ProjectDiagnostic> diagnostics;

    /** Stores a possibly partial catalog and all ordered diagnostics. */
    ExtensionCatalogLoadResult(RegisteredTypeCatalog catalog, List<ProjectDiagnostic> diagnostics) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Returns every successfully resolved extension and registered type.
     *
     * @return possibly partial registered-type catalog
     */
    public RegisteredTypeCatalog catalog() {
        return catalog;
    }

    /**
     * Returns ordered discovery and validation diagnostics.
     *
     * @return immutable diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns whether every declared project extension was resolved without errors.
     *
     * @return {@code true} when the catalog is complete
     */
    public boolean isComplete() {
        return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }
}
