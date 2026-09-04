/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import java.util.List;

/** Immutable preview of one fully prepared but unpublished import generation. */
public final class ImportPreview {
    private final String fingerprint;
    private final List<ProjectDiagnostic> diagnostics;
    private final List<ImportedArtifactMetadata> artifacts;
    private final long estimatedSize;

    /**
     * Creates one preparation preview.
     *
     * @param fingerprint complete input fingerprint
     * @param diagnostics ordered preparation diagnostics
     * @param artifacts completely staged artifacts
     * @param estimatedSize total staged artifact bytes
     */
    public ImportPreview(
            String fingerprint,
            List<ProjectDiagnostic> diagnostics,
            List<ImportedArtifactMetadata> artifacts,
            long estimatedSize) {
        this.fingerprint = Preconditions.requireSha256(fingerprint, "fingerprint");
        this.diagnostics = List.copyOf(diagnostics);
        this.artifacts = Preconditions.copyUniqueArtifactMetadata(artifacts);
        if (estimatedSize < 0L) {
            throw new IllegalArgumentException("estimatedSize must not be negative: " + estimatedSize);
        }
        this.estimatedSize = estimatedSize;
    }

    /**
     * Returns the complete input fingerprint.
     *
     * @return complete input fingerprint
     */
    public String fingerprint() {
        return fingerprint;
    }

    /**
     * Returns immutable ordered preparation diagnostics.
     *
     * @return immutable ordered preparation diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns immutable staged artifact metadata.
     *
     * @return immutable staged artifact metadata
     */
    public List<ImportedArtifactMetadata> artifacts() {
        return artifacts;
    }

    /**
     * Returns the total number of staged artifact bytes.
     *
     * @return total staged artifact bytes
     */
    public long estimatedSize() {
        return estimatedSize;
    }

    /**
     * Returns whether no terminal diagnostic prevents publication.
     *
     * @return {@code true} when no terminal diagnostic prevents publication
     */
    public boolean isValid() {
        return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }
}
