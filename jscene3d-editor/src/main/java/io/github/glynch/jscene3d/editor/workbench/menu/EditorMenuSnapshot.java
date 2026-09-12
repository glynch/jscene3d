/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.menu;

import io.github.glynch.jscene3d.editor.menu.EditorMenuContribution;
import java.util.List;
import java.util.Objects;

/** Immutable top-level menu and ordered command snapshot. */
public record EditorMenuSnapshot(EditorMenuContribution contribution, List<EditorMenuCommandSnapshot> commands) {
    /** Copies one complete top-level menu snapshot. */
    public EditorMenuSnapshot {
        Objects.requireNonNull(contribution, "contribution");
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
    }
}
