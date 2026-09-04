/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable read-only evaluation of one import definition's published state. */
public final class ImportStatus {
    private final ImportState state;
    private final Optional<String> publishedFingerprint;
    private final List<ProjectDiagnostic> diagnostics;

    /**
     * Creates one status result.
     *
     * @param state evaluated state
     * @param publishedFingerprint active generation fingerprint when one exists
     * @param diagnostics ordered status diagnostics
     */
    public ImportStatus(ImportState state, Optional<String> publishedFingerprint, List<ProjectDiagnostic> diagnostics) {
        this.state = Objects.requireNonNull(state, "state");
        this.publishedFingerprint = Preconditions.requireOptionalSha256(publishedFingerprint, "publishedFingerprint");
        this.diagnostics = List.copyOf(diagnostics);
        if ((state == ImportState.CURRENT || state == ImportState.STALE) && this.publishedFingerprint.isEmpty()) {
            throw new IllegalArgumentException("current and stale states require a published fingerprint");
        }
        if (state == ImportState.MISSING && this.publishedFingerprint.isPresent()) {
            throw new IllegalArgumentException("missing state cannot have a published fingerprint");
        }
    }

    /**
     * Returns the evaluated import state.
     *
     * @return evaluated import state
     */
    public ImportState state() {
        return state;
    }

    /**
     * Returns the active published generation fingerprint when present.
     *
     * @return active published generation fingerprint when present
     */
    public Optional<String> publishedFingerprint() {
        return publishedFingerprint;
    }

    /**
     * Returns immutable ordered diagnostics.
     *
     * @return immutable ordered diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }
}
