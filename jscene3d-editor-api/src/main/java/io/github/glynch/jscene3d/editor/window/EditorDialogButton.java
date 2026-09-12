/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

import java.util.Objects;

/**
 * Declares one toolkit-independent modal-dialog action.
 *
 * @param id stable action identity
 * @param title non-blank user-facing button title
 * @param role semantic platform presentation role
 * @param behavior toolkit-independent activation behavior
 */
public record EditorDialogButton(
        EditorDialogButtonId id, String title, EditorDialogButtonRole role, EditorDialogButtonBehavior behavior) {
    /** Creates a conventional action which closes the dialog. */
    public EditorDialogButton(EditorDialogButtonId id, String title, EditorDialogButtonRole role) {
        this(id, title, role, EditorDialogButtonBehavior.CLOSE);
    }

    /** Validates one modal-dialog action. */
    public EditorDialogButton {
        Objects.requireNonNull(id, "id");
        if (Objects.requireNonNull(title, "title").isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(behavior, "behavior");
    }
}
