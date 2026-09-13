/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import java.util.Objects;
import java.util.Set;

/**
 * Toolkit-independent foreground color and emphasis for one semantic syntax category.
 *
 * @param foreground syntax foreground color
 * @param fontStyles immutable font-emphasis styles
 */
public record EditorSyntaxStyle(EditorColor foreground, Set<EditorFontStyle> fontStyles) {
    /** Copies and validates the syntax presentation. */
    public EditorSyntaxStyle {
        Objects.requireNonNull(foreground, "foreground");
        fontStyles = Set.copyOf(Objects.requireNonNull(fontStyles, "fontStyles"));
    }

    /**
     * Creates an un-emphasized syntax presentation.
     *
     * @param foreground syntax foreground color
     * @return plain syntax presentation
     */
    public static EditorSyntaxStyle plain(EditorColor foreground) {
        return new EditorSyntaxStyle(foreground, Set.of());
    }
}
