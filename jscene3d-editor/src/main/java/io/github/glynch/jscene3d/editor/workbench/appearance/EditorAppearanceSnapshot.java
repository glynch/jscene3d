/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import java.util.Objects;

/** Current resolved color theme and source-editor typography. */
public record EditorAppearanceSnapshot(
        EditorResolvedColorTheme colorTheme, String editorFontFamily, int editorFontSize) {
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
