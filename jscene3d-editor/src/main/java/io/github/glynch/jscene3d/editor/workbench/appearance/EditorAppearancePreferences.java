/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;

/** User-level storage boundary for appearance choices. */
public interface EditorAppearancePreferences {
    /**
     * Returns the selected color theme.
     *
     * @param fallback theme used when no preference has been saved
     * @return selected theme
     */
    EditorColorThemeId colorTheme(EditorColorThemeId fallback);

    /**
     * Returns the preferred dark theme.
     *
     * @param fallback theme used when no preference has been saved
     * @return preferred dark theme
     */
    EditorColorThemeId preferredDarkTheme(EditorColorThemeId fallback);

    /**
     * Returns the preferred light theme.
     *
     * @param fallback theme used when no preference has been saved
     * @return preferred light theme
     */
    EditorColorThemeId preferredLightTheme(EditorColorThemeId fallback);

    /**
     * Returns the source-editor font family.
     *
     * @param fallback family used when no preference has been saved
     * @return preferred font family
     */
    String editorFontFamily(String fallback);

    /**
     * Returns the source-editor font size.
     *
     * @param fallback size used when no preference has been saved
     * @return preferred font size in pixels
     */
    int editorFontSize(int fallback);

    /**
     * Saves the selected color theme.
     *
     * @param theme selected theme
     */
    void saveColorTheme(EditorColorThemeId theme);

    /**
     * Saves the preferred dark theme.
     *
     * @param theme preferred dark theme
     */
    void savePreferredDarkTheme(EditorColorThemeId theme);

    /**
     * Saves the preferred light theme.
     *
     * @param theme preferred light theme
     */
    void savePreferredLightTheme(EditorColorThemeId theme);

    /**
     * Saves source-editor typography.
     *
     * @param family font family
     * @param size font size in pixels
     */
    void saveEditorFont(String family, int size);
}
