/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;

/**
 * Result of an internal authoring mutation attempt.
 *
 * @param revision authoritative revision after the attempt
 * @param diagnostics ordered validation diagnostics; any error means the mutation was rejected atomically
 */
public record AuthoringMutationResult(long revision, List<ProjectDiagnostic> diagnostics) {
    /** Copies diagnostics and validates the non-negative revision. */
    public AuthoringMutationResult {
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Returns whether no error diagnostic rejected the mutation.
     *
     * @return whether the mutation was accepted
     */
    public boolean isAccepted() {
        return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }
}
