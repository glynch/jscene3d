/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.diagnostic;

import java.util.Objects;

/**
 * Half-open text range suitable for source diagnostics and navigation.
 *
 * @param start inclusive start position
 * @param end exclusive end position
 */
public record EditorTextRange(EditorTextPosition start, EditorTextPosition end) {
    /** Validates one ordered text range. */
    public EditorTextRange {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("start must not follow end");
        }
    }
}
