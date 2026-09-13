/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable identity of one contributed editor color theme.
 *
 * @param value reverse-domain color-theme identity
 */
public record EditorColorThemeId(String value) {
    /** Validates one reverse-domain color-theme identity. */
    public EditorColorThemeId {
        value = requireNamespacedId(value, "value");
    }
}
