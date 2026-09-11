/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import io.github.glynch.jscene3d.editor.command.CommandId;
import java.util.Objects;
import java.util.Optional;

/**
 * Toolkit-independent presentation of one logical collection element.
 *
 * @param label non-blank primary label
 * @param description optional secondary description
 * @param detail optional supporting detail such as a project-relative path
 * @param tooltip optional accessible detail
 * @param icon optional editor icon identity
 * @param categoryId optional category identity declared by the owning view
 * @param badge optional compact item badge
 * @param command optional command invoked when the item is opened
 * @param contextValue optional semantic value used to select item actions
 */
public record EditorCollectionItem(
        String label,
        Optional<String> description,
        Optional<String> detail,
        Optional<String> tooltip,
        Optional<String> icon,
        Optional<String> categoryId,
        Optional<String> badge,
        Optional<CommandId> command,
        Optional<String> contextValue) {
    /** Copies and validates one item presentation. */
    public EditorCollectionItem {
        if (Objects.requireNonNull(label, "label").isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(tooltip, "tooltip");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(badge, "badge");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(contextValue, "contextValue");
    }
}
