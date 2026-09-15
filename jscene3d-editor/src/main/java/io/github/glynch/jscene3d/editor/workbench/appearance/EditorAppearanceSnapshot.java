/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import java.util.Objects;

/**
 * Current resolved color theme and source-editor typography.
 *
 * @param colorTheme resolved workbench color theme
 * @param editorFontFamily source-editor font family
 * @param editorFontSize source-editor font size in pixels
 */
public record EditorAppearanceSnapshot(
        EditorResolvedColorTheme colorTheme, String editorFontFamily, int editorFontSize) {
    /** Validates the resolved appearance. */
    public EditorAppearanceSnapshot {
        Objects.requireNonNull(colorTheme, "colorTheme");
        if (Objects.requireNonNull(editorFontFamily, "editorFontFamily").isBlank()) {
            throw new IllegalArgumentException("editor font family must not be blank");
        }
        if (editorFontSize < 8 || editorFontSize > 48) {
            throw new IllegalArgumentException("editor font size must be between 8 and 48");
        }
    }
}
