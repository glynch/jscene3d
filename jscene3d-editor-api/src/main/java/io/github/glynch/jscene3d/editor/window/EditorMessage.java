/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

import java.util.Objects;

/**
 * Toolkit-independent message displayed by the editor window.
 *
 * @param severity user-facing message severity
 * @param text non-blank message text
 */
public record EditorMessage(EditorMessageSeverity severity, String text) {
    /** Validates one editor message. */
    public EditorMessage {
        Objects.requireNonNull(severity, "severity");
        if (Objects.requireNonNull(text, "text").isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
    }
}
