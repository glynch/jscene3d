/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Declarative color-theme contribution containing semantic overrides and optional inheritance.
 *
 * @param id stable contribution identity
 * @param label non-blank user-facing label
 * @param kind broad luminance and contrast family
 * @param parent optional theme inherited before applying this theme's overrides
 * @param colors semantic workbench color overrides
 * @param syntaxStyles semantic source-code style overrides
 */
public record EditorColorTheme(
        EditorColorThemeId id,
        String label,
        EditorColorThemeKind kind,
        Optional<EditorColorThemeId> parent,
        Map<EditorColorTokenId, EditorColor> colors,
        Map<EditorSyntaxTokenId, EditorSyntaxStyle> syntaxStyles) {
    /** Copies and validates one color-theme contribution. */
    public EditorColorTheme {
        Objects.requireNonNull(id, "id");
        if (Objects.requireNonNull(label, "label").isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(parent, "parent");
        if (parent.filter(id::equals).isPresent()) {
            throw new IllegalArgumentException("a color theme cannot inherit itself");
        }
        colors = Map.copyOf(Objects.requireNonNull(colors, "colors"));
        syntaxStyles = Map.copyOf(Objects.requireNonNull(syntaxStyles, "syntaxStyles"));
    }
}
