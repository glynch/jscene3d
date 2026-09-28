/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AuthoredDefinitionBackup;
import java.util.Objects;
import java.util.Optional;

/**
 * Structured outcome of capturing one authored-definition recovery backup.
 *
 * @param definition authoritative definition identity
 * @param outcome backup operation outcome
 * @param revision unchanged authoritative content revision
 * @param dirty unchanged authored dirty state
 * @param canUndo unchanged undo availability
 * @param canRedo unchanged redo availability
 * @param backup deterministic recovery state when capture succeeded
 */
public record AuthoringBackupResult(
        AssetId definition,
        Outcome outcome,
        long revision,
        boolean dirty,
        boolean canUndo,
        boolean canRedo,
        Optional<AuthoredDefinitionBackup> backup) {
    /** Validates one complete backup-capture outcome. */
    public AuthoringBackupResult {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(backup, "backup");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        if ((outcome == Outcome.BACKED_UP) != backup.isPresent()) {
            throw new IllegalArgumentException("backup must be present exactly when capture succeeds");
        }
        backup.ifPresent(value -> {
            if (!definition.equals(value.definition())) {
                throw new IllegalArgumentException("backup identity must match result definition");
            }
        });
    }

    /** Returns whether recovery state was captured successfully.
     *
     * @return whether the result contains a backup
     */
    public boolean isAccepted() {
        return outcome == Outcome.BACKED_UP;
    }

    /** Closed expected outcomes for recovery backup capture. */
    public enum Outcome {
        /** Current authored state was captured without changing it or its physical source. */
        BACKED_UP,
        /** The expected content revision was not current. */
        STALE_REVISION,
        /** The retained definition is generated or otherwise non-editable. */
        NON_EDITABLE,
        /** The identity does not name an open authored definition. */
        INVALID_TARGET
    }
}
