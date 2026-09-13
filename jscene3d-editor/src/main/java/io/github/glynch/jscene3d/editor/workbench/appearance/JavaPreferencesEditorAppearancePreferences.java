/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import io.github.glynch.jscene3d.editor.EditorApplication;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;
import java.util.Objects;
import java.util.prefs.Preferences;

/** Persists appearance choices in the operating system's per-user preference store. */
public final class JavaPreferencesEditorAppearancePreferences implements EditorAppearancePreferences {
    private static final String COLOR_THEME = "colorTheme";
    private static final String PREFERRED_DARK_THEME = "preferredDarkTheme";
    private static final String PREFERRED_LIGHT_THEME = "preferredLightTheme";
    private static final String EDITOR_FONT_FAMILY = "editorFontFamily";
    private static final String EDITOR_FONT_SIZE = "editorFontSize";
    private final Preferences preferences;

    /** Creates the production per-user preference adapter. */
    public JavaPreferencesEditorAppearancePreferences() {
        this(Preferences.userNodeForPackage(EditorApplication.class).node("appearance"));
    }

    JavaPreferencesEditorAppearancePreferences(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    @Override
    public EditorColorThemeId colorTheme(EditorColorThemeId fallback) {
        return theme(COLOR_THEME, fallback);
    }

    @Override
    public EditorColorThemeId preferredDarkTheme(EditorColorThemeId fallback) {
        return theme(PREFERRED_DARK_THEME, fallback);
    }

    @Override
    public EditorColorThemeId preferredLightTheme(EditorColorThemeId fallback) {
        return theme(PREFERRED_LIGHT_THEME, fallback);
    }

    private EditorColorThemeId theme(String key, EditorColorThemeId fallback) {
        String stored = preferences.get(key, fallback.value());
        try {
            return new EditorColorThemeId(stored);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    @Override
    public String editorFontFamily(String fallback) {
        String stored = preferences.get(EDITOR_FONT_FAMILY, fallback);
        return stored.isBlank() ? fallback : stored;
    }

    @Override
    public int editorFontSize(int fallback) {
        int stored = preferences.getInt(EDITOR_FONT_SIZE, fallback);
        return stored < 8 || stored > 48 ? fallback : stored;
    }

    @Override
    public void saveColorTheme(EditorColorThemeId theme) {
        preferences.put(COLOR_THEME, Objects.requireNonNull(theme, "theme").value());
    }

    @Override
    public void savePreferredDarkTheme(EditorColorThemeId theme) {
        preferences.put(
                PREFERRED_DARK_THEME, Objects.requireNonNull(theme, "theme").value());
    }

    @Override
    public void savePreferredLightTheme(EditorColorThemeId theme) {
        preferences.put(
                PREFERRED_LIGHT_THEME, Objects.requireNonNull(theme, "theme").value());
    }

    @Override
    public void saveEditorFont(String family, int size) {
        preferences.put(EDITOR_FONT_FAMILY, Objects.requireNonNull(family, "family"));
        preferences.putInt(EDITOR_FONT_SIZE, size);
    }
}
