/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.menu;

import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandState;
import java.util.Objects;

/** Immutable command metadata and state rendered inside one top-level menu. */
public record EditorMenuCommandSnapshot(
        EditorCommandContribution contribution, EditorCommandState state, String group, int order) {
    /** Validates one complete menu-command snapshot. */
    public EditorMenuCommandSnapshot {
        Objects.requireNonNull(contribution, "contribution");
        Objects.requireNonNull(state, "state");
        if (Objects.requireNonNull(group, "group").isBlank()) {
            throw new IllegalArgumentException("group must not be blank");
        }
    }
}
