/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Authoritative recovery-capture result.
 *
 * @param definition authored-definition identity
 * @param outcome stable backup outcome
 * @param revision unchanged authoritative revision
 * @param dirty authoritative dirty state
 * @param canUndo authoritative undo availability
 * @param canRedo authoritative redo availability
 * @param backup Base64-encoded B3 recovery bytes when capture succeeds
 */
public record DefinitionBackupResult(
        String definition,
        String outcome,
        long revision,
        boolean dirty,
        boolean canUndo,
        boolean canRedo,
        @Nullable String backup) {
    /** Validates one complete recovery-capture result. */
    public DefinitionBackupResult {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(outcome, "outcome");
        if (definition.isBlank() || outcome.isBlank()) {
            throw new IllegalArgumentException("definition and outcome must not be blank");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        if ((backup != null) != "backed-up".equals(outcome)) {
            throw new IllegalArgumentException("backup must be present exactly when capture succeeds");
        }
    }
}
