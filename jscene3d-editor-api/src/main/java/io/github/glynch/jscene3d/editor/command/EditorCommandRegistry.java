/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;

/** Registers and invokes commands independently of their visual placement. */
public interface EditorCommandRegistry {
    /**
     * Registers one command implementation.
     *
     * @param contribution stable identity and user-facing metadata
     * @param command executable behavior
     * @return removable registration
     * @throws IllegalArgumentException if the identity is already registered
     */
    EditorRegistration register(EditorCommandContribution contribution, EditorCommand command);

    /**
     * Invokes one registered command through the editor's ordinary dispatch path.
     *
     * @param command command identity
     * @throws IllegalArgumentException if no command has the supplied identity
     */
    void execute(CommandId command);
}
