/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

/**
 * Dynamic presentation state shared by every placement of one command.
 *
 * @param enabled whether the command may currently be invoked
 * @param selected whether a toggle command is currently selected
 */
public record EditorCommandState(boolean enabled, boolean selected) {
    /** Enabled command state. */
    public static final EditorCommandState ENABLED_STATE = new EditorCommandState(true, false);

    /** Disabled command state. */
    public static final EditorCommandState DISABLED_STATE = new EditorCommandState(false, false);

    /**
     * Creates an unselected ordinary command state.
     *
     * @param enabled whether the command may currently be invoked
     */
    public EditorCommandState(boolean enabled) {
        this(enabled, false);
    }
}
