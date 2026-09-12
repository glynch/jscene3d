/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

/** Dynamic presentation state shared by every placement of one command. */
public record EditorCommandState(boolean enabled) {
    /** Enabled command state. */
    public static final EditorCommandState ENABLED_STATE = new EditorCommandState(true);

    /** Disabled command state. */
    public static final EditorCommandState DISABLED_STATE = new EditorCommandState(false);
}
