/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

/** Executable behavior bound to one stable editor command identity. */
@FunctionalInterface
public interface EditorCommand {
    /**
     * Executes on the editor-owned command dispatch thread.
     *
     * @param context invocation-scoped editor facilities
     */
    void execute(EditorCommandContext context);
}
