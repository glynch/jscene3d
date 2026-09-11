/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.diagnostic;

/**
 * Zero-based text position within an editor document.
 *
 * @param line zero-based line
 * @param character zero-based UTF-16 code-unit offset within the line
 */
public record EditorTextPosition(int line, int character) implements Comparable<EditorTextPosition> {
    /** Validates one non-negative text position. */
    public EditorTextPosition {
        if (line < 0 || character < 0) {
            throw new IllegalArgumentException("line and character must not be negative");
        }
    }

    /** Orders positions by line and then character. */
    @Override
    public int compareTo(EditorTextPosition other) {
        int lineOrder = Integer.compare(line, other.line);
        return lineOrder != 0 ? lineOrder : Integer.compare(character, other.character);
    }
}
