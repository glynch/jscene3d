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
 * Structured outcome of saving or reverting one authored-definition working copy.
 *
 * <p>Every outcome reports the unchanged or resulting authoritative lifecycle state. Expected concurrency,
 * editability, external-source, validation, and persistence failures are values rather than exceptions.
 *
 * @param definition authoritative definition identity
 * @param outcome persistence operation outcome
 * @param revision authoritative monotonic content revision after the attempt
 * @param dirty whether current authored state differs from persisted authored state
 * @param canUndo whether an earlier accepted state is available
 * @param canRedo whether a previously undone state is available
 * @param diagnostics ordered persistence or validation diagnostics
 */
public record AuthoringPersistenceResult(
        AssetId definition,
        Outcome outcome,
        long revision,
        boolean dirty,
        boolean canUndo,
        boolean canRedo,
        List<ProjectDiagnostic> diagnostics) {
    /** Copies diagnostics and validates the complete persistence outcome. */
    public AuthoringPersistenceResult {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(outcome, "outcome");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        diagnostics = List.copyOf(diagnostics);
        if (outcome.isSuccessful() && !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("successful persistence outcomes cannot carry diagnostics");
        }
    }

    /**
     * Returns whether the requested persistence operation completed successfully.
     *
     * @return whether save or revert was accepted
     */
    public boolean isAccepted() {
        return outcome.isSuccessful();
    }

    /** Closed expected outcomes for authored-definition persistence operations. */
    public enum Outcome {
        /** Dirty authored state was persisted atomically. */
        SAVED,
        /** Current physical source was accepted as the authoritative persisted state. */
        REVERTED,
        /** The requested operation required no state or disk transition. */
        NO_OP,
        /** The expected content revision was not current. */
        STALE_REVISION,
        /** The retained definition is generated or otherwise non-editable. */
        NON_EDITABLE,
        /** The identity does not name an open authored definition. */
        INVALID_TARGET,
        /** Physical source bytes differ from the expected persisted fingerprint. */
        SOURCE_CHANGED,
        /** The retained physical source no longer exists. */
        SOURCE_MISSING,
        /** The retained physical source cannot be accessed as a regular readable file. */
        SOURCE_INACCESSIBLE,
        /** The retained physical source resolves outside its trusted project root. */
        SOURCE_OUTSIDE_PROJECT,
        /** The atomic writer could not replace the verified source. */
        WRITE_FAILED,
        /** Reloaded physical source failed normal parsing or authoritative validation. */
        VALIDATION_REJECTED;

        /** Returns whether this outcome accepted the persistence request. */
        private boolean isSuccessful() {
            return this == SAVED || this == REVERTED || this == NO_OP;
        }
    }
}
