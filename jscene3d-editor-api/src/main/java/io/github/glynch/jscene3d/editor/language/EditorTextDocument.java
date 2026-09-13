/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import java.net.URI;
import java.util.Objects;

/**
 * Immutable, versioned snapshot of an open editor text document.
 *
 * @param resource canonical document URI
 * @param language editor language identity
 * @param version positive, monotonically increasing working-copy version
 * @param text authoritative in-memory content
 */
public record EditorTextDocument(URI resource, EditorLanguageId language, int version, String text) {
    /** Validates one document snapshot. */
    public EditorTextDocument {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(language, "language");
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        Objects.requireNonNull(text, "text");
    }
}
