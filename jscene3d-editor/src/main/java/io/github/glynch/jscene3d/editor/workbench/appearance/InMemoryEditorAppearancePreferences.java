/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Non-persistent appearance preferences used by isolated workbench hosts and tests. */
public final class InMemoryEditorAppearancePreferences implements EditorAppearancePreferences {
    private @Nullable EditorColorThemeId theme;
    private @Nullable EditorColorThemeId preferredDarkTheme;
    private @Nullable EditorColorThemeId preferredLightTheme;
    private @Nullable String family;
    private @Nullable Integer size;

    @Override
    public EditorColorThemeId colorTheme(EditorColorThemeId fallback) {
        return theme == null ? fallback : theme;
    }

    @Override
    public EditorColorThemeId preferredDarkTheme(EditorColorThemeId fallback) {
        return preferredDarkTheme == null ? fallback : preferredDarkTheme;
    }

    @Override
    public EditorColorThemeId preferredLightTheme(EditorColorThemeId fallback) {
        return preferredLightTheme == null ? fallback : preferredLightTheme;
    }

    @Override
    public String editorFontFamily(String fallback) {
        return family == null ? fallback : family;
    }

    @Override
    public int editorFontSize(int fallback) {
        return size == null ? fallback : size;
    }

    @Override
    public void saveColorTheme(EditorColorThemeId selected) {
        theme = Objects.requireNonNull(selected, "selected");
    }

    @Override
    public void savePreferredDarkTheme(EditorColorThemeId selected) {
        preferredDarkTheme = Objects.requireNonNull(selected, "selected");
    }

    @Override
    public void savePreferredLightTheme(EditorColorThemeId selected) {
        preferredLightTheme = Objects.requireNonNull(selected, "selected");
    }

    @Override
    public void saveEditorFont(String selectedFamily, int selectedSize) {
        family = Objects.requireNonNull(selectedFamily, "selectedFamily");
        size = selectedSize;
    }
}
