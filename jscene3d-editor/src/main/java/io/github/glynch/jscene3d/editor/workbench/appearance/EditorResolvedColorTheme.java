/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import io.github.glynch.jscene3d.editor.theme.EditorColor;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeKind;
import io.github.glynch.jscene3d.editor.theme.EditorColorTokenId;
import io.github.glynch.jscene3d.editor.theme.EditorSyntaxStyle;
import io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokenId;
import java.util.Map;
import java.util.Objects;

/**
 * Complete effective theme after inheritance has been resolved.
 *
 * @param id stable theme identity
 * @param label display label
 * @param kind light or dark theme kind
 * @param colors resolved workbench colors by token
 * @param syntaxStyles resolved source syntax styles by token
 */
public record EditorResolvedColorTheme(
        EditorColorThemeId id,
        String label,
        EditorColorThemeKind kind,
        Map<EditorColorTokenId, EditorColor> colors,
        Map<EditorSyntaxTokenId, EditorSyntaxStyle> syntaxStyles) {
    public EditorResolvedColorTheme {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(kind, "kind");
        colors = Map.copyOf(Objects.requireNonNull(colors, "colors"));
        syntaxStyles = Map.copyOf(Objects.requireNonNull(syntaxStyles, "syntaxStyles"));
    }

    /** Returns one required standard workbench color. */
    public EditorColor color(EditorColorTokenId token) {
        EditorColor color = colors.get(Objects.requireNonNull(token, "token"));
        if (color == null) {
            throw new IllegalArgumentException("resolved theme does not define color token: " + token.value());
        }
        return color;
    }
}
