/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopyId;
import java.util.Objects;

/**
 * Describes one undoable edit and the resource it affects.
 *
 * @param label concise user-facing edit label
 * @param workingCopyId affected resource working-copy identity
 */
public record EditorUndoRedoEntry(String label, EditorWorkingCopyId workingCopyId) {
    /** Validates the complete undo/redo description. */
    public EditorUndoRedoEntry {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(workingCopyId, "workingCopyId");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
    }
}
