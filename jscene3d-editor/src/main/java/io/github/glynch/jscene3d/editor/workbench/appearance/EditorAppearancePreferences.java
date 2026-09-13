/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;

/** User-level storage boundary for appearance choices. */
public interface EditorAppearancePreferences {
    EditorColorThemeId colorTheme(EditorColorThemeId fallback);

    EditorColorThemeId preferredDarkTheme(EditorColorThemeId fallback);

    EditorColorThemeId preferredLightTheme(EditorColorThemeId fallback);

    String editorFontFamily(String fallback);

    int editorFontSize(int fallback);

    void saveColorTheme(EditorColorThemeId theme);

    void savePreferredDarkTheme(EditorColorThemeId theme);

    void savePreferredLightTheme(EditorColorThemeId theme);

    void saveEditorFont(String family, int size);
}
