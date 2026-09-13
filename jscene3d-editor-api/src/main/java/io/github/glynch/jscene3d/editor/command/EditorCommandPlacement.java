/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import java.util.Objects;
import java.util.Optional;

/**
 * Displays a registered command on one editor-owned interaction surface.
 *
 * @param command registered command identity
 * @param location target menu, toolbar, context menu, or other action surface
 * @param group non-blank group used to insert separators between adjacent command groups
 * @param order ascending presentation order within the group
 * @param contextValue optional semantic item context required for this placement
 */
public record EditorCommandPlacement(
        CommandId command, CommandLocationId location, String group, int order, Optional<String> contextValue) {
    /** Validates one command placement. */
    public EditorCommandPlacement {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(location, "location");
        if (Objects.requireNonNull(group, "group").isBlank()) {
            throw new IllegalArgumentException("group must not be blank");
        }
        Objects.requireNonNull(contextValue, "contextValue").ifPresent(value -> {
            if (value.isBlank()) {
                throw new IllegalArgumentException("contextValue must not be blank");
            }
        });
    }

    /**
     * Creates a placement which is available for every context at its location.
     *
     * @param command registered command identity
     * @param location target interaction surface
     * @param group non-blank presentation group
     * @param order ascending presentation order
     */
    public EditorCommandPlacement(CommandId command, CommandLocationId location, String group, int order) {
        this(command, location, group, order, Optional.empty());
    }

    /**
     * Creates a placement in the default command group.
     *
     * @param command registered command identity
     * @param location target interaction surface
     * @param order ascending presentation order
     */
    public EditorCommandPlacement(CommandId command, CommandLocationId location, int order) {
        this(command, location, "default", order);
    }

    /**
     * Creates a placement available only for items with the supplied semantic context value.
     *
     * @param command registered command identity
     * @param location target interaction surface
     * @param group non-blank presentation group
     * @param order ascending presentation order
     * @param contextValue semantic item context required for availability
     */
    public EditorCommandPlacement(
            CommandId command, CommandLocationId location, String group, int order, String contextValue) {
        this(command, location, group, order, Optional.of(Objects.requireNonNull(contextValue, "contextValue")));
    }
}
