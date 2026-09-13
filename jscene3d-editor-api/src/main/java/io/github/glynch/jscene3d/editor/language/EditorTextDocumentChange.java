/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import java.util.List;
import java.util.Objects;

/**
 * Versioned text-document snapshot plus the ordered incremental edits which produced it.
 *
 * @param document document state after applying the edits
 * @param edits ordered UTF-16 edits reported by the source editor
 */
public record EditorTextDocumentChange(EditorTextDocument document, List<EditorTextEdit> edits) {
    /** Copies and validates one change notification. */
    public EditorTextDocumentChange {
        Objects.requireNonNull(document, "document");
        edits = List.copyOf(Objects.requireNonNull(edits, "edits"));
        if (edits.isEmpty()) {
            throw new IllegalArgumentException("edits must not be empty");
        }
    }
}
