/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.status;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import java.util.Objects;
import java.util.Optional;

/**
 * Complete atomic presentation state of one status-bar item.
 *
 * @param text non-blank visible text
 * @param icon optional semantic icon
 * @param tooltip optional accessible detail
 * @param command optional command invoked when the item is selected
 * @param visible whether the item participates in status-bar layout
 */
public record EditorStatusItemState(
        String text,
        Optional<EditorIcon> icon,
        Optional<String> tooltip,
        Optional<CommandId> command,
        boolean visible) {
    /** Copies and validates one status-item state. */
    public EditorStatusItemState {
        if (Objects.requireNonNull(text, "text").isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(tooltip, "tooltip");
        Objects.requireNonNull(command, "command");
    }
}
