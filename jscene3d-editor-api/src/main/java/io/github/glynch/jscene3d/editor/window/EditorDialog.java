/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Toolkit-independent application-modal dialog declaration.
 *
 * @param title non-blank window title
 * @param heading non-blank dialog heading
 * @param content plain-text dialog content
 * @param buttons ordered non-empty dialog actions
 */
public record EditorDialog(String title, String heading, String content, List<EditorDialogButton> buttons) {
    /** Copies and validates one complete dialog declaration. */
    public EditorDialog {
        requireNotBlank(title, "title");
        requireNotBlank(heading, "heading");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(buttons, "buttons");
        if (buttons.isEmpty()) {
            throw new IllegalArgumentException("buttons must not be empty");
        }
        if (buttons.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("buttons must not contain null");
        }
        Set<EditorDialogButtonId> identities = new HashSet<>();
        if (buttons.stream().map(EditorDialogButton::id).anyMatch(id -> !identities.add(id))) {
            throw new IllegalArgumentException("buttons must not contain duplicate identities");
        }
        requireAtMostOneRole(buttons, EditorDialogButtonRole.DEFAULT);
        requireAtMostOneRole(buttons, EditorDialogButtonRole.CANCEL);
        buttons = List.copyOf(buttons);
    }

    private static void requireNotBlank(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireAtMostOneRole(List<EditorDialogButton> buttons, EditorDialogButtonRole role) {
        if (buttons.stream().filter(button -> button.role() == role).count() > 1) {
            throw new IllegalArgumentException("buttons must contain at most one " + role + " action");
        }
    }
}
