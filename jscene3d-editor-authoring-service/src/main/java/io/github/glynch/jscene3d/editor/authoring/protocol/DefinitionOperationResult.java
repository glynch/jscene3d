/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;

/** Authoritative state returned after a mutation, history, persistence, or recovery operation.
 *
 * @param definition authored-definition identity
 * @param outcome stable operation outcome
 * @param revision authoritative current revision
 * @param dirty authoritative dirty state
 * @param canUndo authoritative undo availability
 * @param canRedo authoritative redo availability
 * @param diagnostics localized authoritative diagnostics
 */
public record DefinitionOperationResult(
        String definition,
        String outcome,
        long revision,
        boolean dirty,
        boolean canUndo,
        boolean canRedo,
        List<ProjectDiagnosticDto> diagnostics) {
    /** Copies and validates one operation result. */
    public DefinitionOperationResult {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(outcome, "outcome");
        diagnostics = List.copyOf(diagnostics);
        if (definition.isBlank() || outcome.isBlank()) {
            throw new IllegalArgumentException("definition and outcome must not be blank");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }
}
