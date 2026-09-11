/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

import io.github.glynch.jscene3d.editor.command.CommandId;
import java.util.Objects;
import java.util.Optional;

/**
 * Toolkit-independent message displayed by the editor window.
 *
 * @param severity user-facing message severity
 * @param text non-blank message text
 * @param command optional command invoked when the user activates the message
 */
public record EditorMessage(EditorMessageSeverity severity, String text, Optional<CommandId> command) {
    /** Validates one editor message. */
    public EditorMessage {
        Objects.requireNonNull(severity, "severity");
        if (Objects.requireNonNull(text, "text").isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        Objects.requireNonNull(command, "command");
    }

    /** Creates a message without an associated command. */
    public EditorMessage(EditorMessageSeverity severity, String text) {
        this(severity, text, Optional.empty());
    }

    /** Creates an actionable message backed by a registered editor command. */
    public EditorMessage(EditorMessageSeverity severity, String text, CommandId command) {
        this(severity, text, Optional.of(Objects.requireNonNull(command, "command")));
    }
}
