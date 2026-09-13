/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

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
    EditorCommandRegistration register(EditorCommandContribution contribution, EditorCommand command);

    /**
     * Invokes one registered command through the editor's ordinary dispatch path.
     *
     * @param command command identity
     * @throws IllegalArgumentException if no command has the supplied identity
     */
    void execute(CommandId command);

    /**
     * Invokes one registered command with the semantic argument from an item-oriented interaction.
     *
     * @param command command identity
     * @param argument exact logical item or resource involved in the invocation
     * @throws IllegalArgumentException if no command has the supplied identity
     */
    void execute(CommandId command, Object argument);
}
