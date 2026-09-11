/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import io.github.glynch.jscene3d.editor.command.CommandId;
import java.util.Objects;
import java.util.Optional;

/**
 * Toolkit-independent presentation of one logical tree element.
 *
 * @param label non-blank primary label
 * @param description optional secondary description
 * @param tooltip optional accessible detail
 * @param icon optional editor icon identity
 * @param command optional command invoked when the item is opened
 * @param contextValue optional semantic value used to select item actions
 * @param collapsibleState initial child-expansion capability and state
 */
public record EditorTreeItem(
        String label,
        Optional<String> description,
        Optional<String> tooltip,
        Optional<String> icon,
        Optional<CommandId> command,
        Optional<String> contextValue,
        EditorTreeItemCollapsibleState collapsibleState) {
    /** Copies and validates one tree-item presentation. */
    public EditorTreeItem {
        if (Objects.requireNonNull(label, "label").isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(tooltip, "tooltip");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(contextValue, "contextValue");
        Objects.requireNonNull(collapsibleState, "collapsibleState");
    }

    /**
     * Creates a plain leaf item.
     *
     * @param label non-blank primary label
     * @return leaf item without optional presentation values
     */
    public static EditorTreeItem leaf(String label) {
        return new EditorTreeItem(
                label,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                EditorTreeItemCollapsibleState.NONE);
    }
}
