/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import java.util.Objects;

/**
 * One UTF-16 source-range replacement in a text-document change.
 *
 * @param range half-open range in the content before the change
 * @param text replacement text
 */
public record EditorTextEdit(EditorTextRange range, String text) {
    /** Validates one incremental edit. */
    public EditorTextEdit {
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(text, "text");
    }
}
