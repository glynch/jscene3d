/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable read-only description of selectable source content and dependencies. */
public final class SourceInspection {
    private final RegisteredType importer;
    private final String sourceFingerprint;
    private final Map<Path, String> dependencies;
    private final List<ProjectDiagnostic> diagnostics;
    private final List<SourceItem> items;

    /**
     * Creates one completed source inspection.
     *
     * @param importer exact importer type and version
     * @param sourceFingerprint lowercase source SHA-256 fingerprint
     * @param dependencies normalized absolute dependency paths and lowercase SHA-256 fingerprints
     * @param diagnostics ordered inspection diagnostics
     * @param items discovered source items
     */
    public SourceInspection(
            RegisteredType importer,
            String sourceFingerprint,
            Map<Path, String> dependencies,
            List<ProjectDiagnostic> diagnostics,
            List<SourceItem> items) {
        this.importer = Objects.requireNonNull(importer, "importer");
        this.sourceFingerprint = Preconditions.requireSha256(sourceFingerprint, "sourceFingerprint");
        this.dependencies = Preconditions.copyFingerprints(dependencies, "dependencies");
        this.diagnostics = List.copyOf(diagnostics);
        this.items = Preconditions.copyUniqueSourceItems(items);
    }

    /**
     * Returns the exact importer type and version.
     *
     * @return exact importer type and version
     */
    public RegisteredType importer() {
        return importer;
    }

    /**
     * Returns the lowercase source SHA-256 fingerprint.
     *
     * @return lowercase source SHA-256 fingerprint
     */
    public String sourceFingerprint() {
        return sourceFingerprint;
    }

    /**
     * Returns the immutable dependency fingerprint index.
     *
     * @return immutable dependency fingerprint index
     */
    public Map<Path, String> dependencies() {
        return dependencies;
    }

    /**
     * Returns immutable ordered diagnostics.
     *
     * @return immutable ordered diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns immutable discovered source items.
     *
     * @return immutable discovered source items
     */
    public List<SourceItem> items() {
        return items;
    }

    /**
     * Returns whether inspection completed without a terminal diagnostic.
     *
     * @return {@code true} when no terminal diagnostic was reported
     */
    public boolean isValid() {
        return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }
}
