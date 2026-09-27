/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.project.asset.AssetId;
import java.util.Objects;

/**
 * Semantic notification emitted after one authored working-copy state transition.
 *
 * @param definition authoritative authored-definition identity
 * @param revision current monotonic content revision
 * @param dirty whether current authored state differs from persisted authored state
 * @param canUndo whether an earlier accepted state is available
 * @param canRedo whether a previously undone state is available
 * @param kind state transition kind
 */
public record AuthoringDefinitionChange(
        AssetId definition, long revision, boolean dirty, boolean canUndo, boolean canRedo, Kind kind) {
    /** Validates one coherent definition-change notification. */
    public AuthoringDefinitionChange {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(kind, "kind");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }

    /** Authored working-copy transition kinds. */
    public enum Kind {
        /** One SET transaction changed authored state. */
        SET,
        /** One REMOVE transaction changed authored state. */
        REMOVE,
        /** One prior transaction was undone. */
        UNDO,
        /** One prior transaction was redone. */
        REDO,
        /** Current authored state became the in-memory persisted baseline without disk I/O. */
        PERSISTED
    }
}
