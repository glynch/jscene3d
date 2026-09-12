/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import java.util.Objects;

/**
 * Displays a registered command on one editor-owned interaction surface.
 *
 * @param command registered command identity
 * @param location target menu, toolbar, context menu, or other action surface
 * @param group non-blank group used to insert separators between adjacent command groups
 * @param order ascending presentation order within the group
 */
public record EditorCommandPlacement(CommandId command, CommandLocationId location, String group, int order) {
    /** Validates one command placement. */
    public EditorCommandPlacement {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(location, "location");
        if (Objects.requireNonNull(group, "group").isBlank()) {
            throw new IllegalArgumentException("group must not be blank");
        }
    }

    /** Creates a placement in the default command group. */
    public EditorCommandPlacement(CommandId command, CommandLocationId location, int order) {
        this(command, location, "default", order);
    }
}
