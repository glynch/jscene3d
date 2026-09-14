/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import java.util.Objects;

/**
 * User-facing metadata for one executable command.
 *
 * @param id stable command identity
 * @param title non-blank human-readable title
 * @param kind visual interaction kind
 */
public record EditorCommandContribution(CommandId id, String title, EditorCommandKind kind) {
    /** Validates one command contribution. */
    public EditorCommandContribution {
        Objects.requireNonNull(id, "id");
        if (Objects.requireNonNull(title, "title").isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        Objects.requireNonNull(kind, "kind");
    }

    /**
     * Creates an ordinary action command.
     *
     * @param id stable command identity
     * @param title non-blank human-readable title
     */
    public EditorCommandContribution(CommandId id, String title) {
        this(id, title, EditorCommandKind.ACTION);
    }
}
