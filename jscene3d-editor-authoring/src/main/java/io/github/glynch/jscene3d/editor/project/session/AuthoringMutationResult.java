/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;

/**
 * Structured outcome of one authored-definition mutation or history operation.
 *
 * <p>Every outcome reports the unchanged or resulting authoritative working-copy state. Expected validation,
 * concurrency, editability, target, and history-availability failures are values rather than exceptions.
 *
 * @param definition authoritative authored-definition identity
 * @param outcome operation outcome
 * @param revision authoritative monotonic revision after the attempt
 * @param dirty whether current authored state differs from the persisted authored state
 * @param canUndo whether an earlier accepted state is available
 * @param canRedo whether a previously undone state is available
 * @param diagnostics ordered validation diagnostics
 */
public record AuthoringMutationResult(
        AssetId definition,
        Outcome outcome,
        long revision,
        boolean dirty,
        boolean canUndo,
        boolean canRedo,
        List<ProjectDiagnostic> diagnostics) {
    /** Copies diagnostics and validates the complete state outcome. */
    public AuthoringMutationResult {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(outcome, "outcome");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        diagnostics = List.copyOf(diagnostics);
        if (outcome == Outcome.VALIDATION_REJECTED && diagnostics.isEmpty()) {
            throw new IllegalArgumentException("validation rejection requires diagnostics");
        }
        if (outcome != Outcome.VALIDATION_REJECTED && !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("only validation rejection carries diagnostics");
        }
    }

    /**
     * Returns whether the requested operation was accepted, including an accepted no-op.
     *
     * @return whether the mutation was accepted
     */
    public boolean isAccepted() {
        return outcome == Outcome.ACCEPTED || outcome == Outcome.NO_OP;
    }

    /** Closed expected outcomes for authored-definition operations. */
    public enum Outcome {
        /** Authored state changed atomically. */
        ACCEPTED,
        /** The requested operation was valid but changed no authored state. */
        NO_OP,
        /** Complete candidate validation rejected the operation atomically. */
        VALIDATION_REJECTED,
        /** The expected definition revision was not current. */
        STALE_REVISION,
        /** The supplied semantic target did not identify current source-local content. */
        INVALID_TARGET,
        /** The retained definition is generated or otherwise non-editable. */
        NON_EDITABLE,
        /** No earlier accepted transaction is available. */
        UNDO_UNAVAILABLE,
        /** No previously undone transaction is available. */
        REDO_UNAVAILABLE
    }
}
