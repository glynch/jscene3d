/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import java.util.Objects;

/**
 * Immutable current lifecycle state of one authored definition working copy.
 *
 * @param definition authoritative authored-definition identity
 * @param kind world or entity definition kind
 * @param revision current monotonic content revision
 * @param dirty whether current authored state differs from persisted authored state
 * @param canUndo whether an earlier accepted state is available
 * @param canRedo whether a previously undone state is available
 * @param content current immutable domain projection
 */
public record AuthoringDefinitionState(
        AssetId definition,
        AssetKind kind,
        long revision,
        boolean dirty,
        boolean canUndo,
        boolean canRedo,
        EditorRetainedDefinition.Content content) {
    /** Validates one coherent authored-definition state snapshot. */
    public AuthoringDefinitionState {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(content, "content");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        if (!definition.equals(content.id()) || kind != content.kind()) {
            throw new IllegalArgumentException("definition identity and content must agree");
        }
    }
}
