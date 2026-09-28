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
 * Structured outcome of restoring authored recovery state into a newly loaded working copy.
 *
 * <p>Every outcome reports the unchanged or resulting lifecycle state. Expected format, identity, source-baseline,
 * validation, concurrency, and editability failures are values rather than exceptions.
 *
 * @param definition authoritative definition identity
 * @param outcome restore operation outcome
 * @param revision authoritative new-session content revision after the attempt
 * @param dirty whether current authored state differs from its persisted semantic baseline
 * @param canUndo whether an earlier new-session transaction is available
 * @param canRedo whether a previously undone new-session transaction is available
 * @param diagnostics ordered authoritative validation diagnostics
 */
public record AuthoringRestoreResult(
        AssetId definition,
        Outcome outcome,
        long revision,
        boolean dirty,
        boolean canUndo,
        boolean canRedo,
        List<ProjectDiagnostic> diagnostics) {
    /** Copies diagnostics and validates one complete recovery outcome. */
    public AuthoringRestoreResult {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(outcome, "outcome");
        diagnostics = List.copyOf(diagnostics);
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        if (outcome != Outcome.VALIDATION_REJECTED && !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("only validation rejection may carry diagnostics");
        }
        if (outcome == Outcome.VALIDATION_REJECTED && diagnostics.isEmpty()) {
            throw new IllegalArgumentException("validation rejection requires diagnostics");
        }
    }

    /** Returns whether recovery state was accepted.
     *
     * @return whether restore succeeded or required no transition
     */
    public boolean isAccepted() {
        return outcome == Outcome.RESTORED || outcome == Outcome.NO_OP;
    }

    /** Closed expected outcomes for authored recovery restoration. */
    public enum Outcome {
        /** Validated backup content replaced current authored state. */
        RESTORED,
        /** Backup content already matched current authored state. */
        NO_OP,
        /** The expected new-session content revision was not current. */
        STALE_REVISION,
        /** The retained definition is generated or otherwise non-editable. */
        NON_EDITABLE,
        /** The identity does not name an open authored definition. */
        INVALID_TARGET,
        /** Backup bytes are not one complete strict JSON object. */
        BACKUP_MALFORMED,
        /** The explicit backup format marker or version is unsupported. */
        BACKUP_UNSUPPORTED,
        /** Required backup structure or identity values are invalid. */
        BACKUP_INVALID,
        /** Stable owning project identity differs. */
        PROJECT_MISMATCH,
        /** Authored asset identity differs. */
        DEFINITION_MISMATCH,
        /** Authored structural kind differs. */
        KIND_MISMATCH,
        /** Portable trusted source identity differs. */
        SOURCE_MISMATCH,
        /** Newly loaded physical-source fingerprint differs from the backup baseline. */
        SOURCE_CHANGED,
        /** Backup content failed normal authoritative definition validation. */
        VALIDATION_REJECTED
    }
}
