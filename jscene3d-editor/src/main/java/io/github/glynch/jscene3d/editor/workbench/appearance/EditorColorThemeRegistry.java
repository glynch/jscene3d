/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.theme.EditorColor;
import io.github.glynch.jscene3d.editor.theme.EditorColorTheme;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeKind;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemes;
import io.github.glynch.jscene3d.editor.theme.EditorColorTokenId;
import io.github.glynch.jscene3d.editor.theme.EditorColorTokens;
import io.github.glynch.jscene3d.editor.theme.EditorSyntaxStyle;
import io.github.glynch.jscene3d.editor.theme.EditorSyntaxTokenId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/** Owns theme contributions, inheritance resolution, selection, and user typography. */
public final class EditorColorThemeRegistry implements EditorColorThemes {
    /** Built-in fallback selected when a stored external theme is not installed. */
    public static final EditorColorThemeId DEFAULT_THEME =
            new EditorColorThemeId("io.github.glynch.jscene3d.editor.theme.dark");

    /** Built-in light fallback used when the preferred light theme is not installed. */
    public static final EditorColorThemeId DEFAULT_LIGHT_THEME =
            new EditorColorThemeId("io.github.glynch.jscene3d.editor.theme.light");

    private static final String DEFAULT_EDITOR_FONT = "Monospaced";
    private static final int DEFAULT_EDITOR_FONT_SIZE = 13;

    private final EditorAppearancePreferences preferences;
    private final Map<EditorColorThemeId, EditorColorTheme> contributions = new LinkedHashMap<>();
    private final List<Consumer<EditorAppearanceSnapshot>> appearanceObservers = new ArrayList<>();
    private final List<Consumer<List<EditorResolvedColorTheme>>> themeObservers = new ArrayList<>();
    private EditorColorThemeId requestedTheme;
    private EditorColorThemeId preferredDarkTheme;
    private EditorColorThemeId preferredLightTheme;
    private String editorFontFamily;
    private int editorFontSize;
    private @Nullable EditorAppearanceSnapshot current;

    /** Creates an empty registry using the supplied user-level preference adapter. */
    public EditorColorThemeRegistry(EditorAppearancePreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        requestedTheme = preferences.colorTheme(DEFAULT_THEME);
        preferredDarkTheme = preferences.preferredDarkTheme(DEFAULT_THEME);
        preferredLightTheme = preferences.preferredLightTheme(DEFAULT_LIGHT_THEME);
        editorFontFamily = preferences.editorFontFamily(DEFAULT_EDITOR_FONT);
        editorFontSize = preferences.editorFontSize(DEFAULT_EDITOR_FONT_SIZE);
    }

    @Override
    public EditorRegistration register(EditorColorTheme theme) {
        EditorColorTheme candidate = Objects.requireNonNull(theme, "theme");
        if (contributions.containsKey(candidate.id())) {
            throw new IllegalArgumentException("color-theme identity is already registered: "
                    + candidate.id().value());
        }
        candidate.parent().ifPresent(parent -> {
            if (!contributions.containsKey(parent)) {
                throw new IllegalArgumentException("color-theme parent is not registered: " + parent.value());
            }
        });
        contributions.put(candidate.id(), candidate);
        try {
            requireComplete(resolve(candidate.id()));
        } catch (RuntimeException failure) {
            contributions.remove(candidate.id());
            throw failure;
        }
        refresh();
        notifyThemeObservers();
        return once(() -> {
            if (contributions.remove(candidate.id(), candidate)) {
                refresh();
                notifyThemeObservers();
            }
        });
    }

    /** Returns the current complete appearance. */
    public EditorAppearanceSnapshot current() {
        EditorAppearanceSnapshot appearance = current;
        if (appearance == null) {
            throw new IllegalStateException("no complete color theme has been registered");
        }
        return appearance;
    }

    /** Returns every currently resolvable theme in contribution order. */
    public List<EditorResolvedColorTheme> themes() {
        return contributions.keySet().stream()
                .map(this::resolveIfPossible)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Selects and persists one registered theme. */
    public void select(EditorColorThemeId theme) {
        EditorColorThemeId selected = Objects.requireNonNull(theme, "theme");
        if (!contributions.containsKey(selected)) {
            throw new IllegalArgumentException("color theme is not registered: " + selected.value());
        }
        rememberPreferredTheme(requireComplete(resolve(selected)));
        requestedTheme = selected;
        preferences.saveColorTheme(selected);
        refresh();
    }

    /** Switches to the preferred theme in the opposite light or dark family. */
    public void toggleColorScheme() {
        select(preferredTheme(!isDark(current().colorTheme().kind())));
    }

    /** Selects and persists source-editor typography independently of the color theme. */
    public void setEditorFont(String family, int size) {
        EditorAppearanceSnapshot validated =
                new EditorAppearanceSnapshot(current().colorTheme(), family, size);
        editorFontFamily = validated.editorFontFamily();
        editorFontSize = validated.editorFontSize();
        preferences.saveEditorFont(editorFontFamily, editorFontSize);
        refresh();
    }

    /** Observes appearance changes and immediately receives the current value. */
    public EditorRegistration observeAppearance(Consumer<EditorAppearanceSnapshot> observer) {
        Consumer<EditorAppearanceSnapshot> listener = Objects.requireNonNull(observer, "observer");
        appearanceObservers.add(listener);
        listener.accept(current());
        return once(() -> appearanceObservers.remove(listener));
    }

    /** Observes available themes and immediately receives the current list. */
    public EditorRegistration observeThemes(Consumer<List<EditorResolvedColorTheme>> observer) {
        Consumer<List<EditorResolvedColorTheme>> listener = Objects.requireNonNull(observer, "observer");
        themeObservers.add(listener);
        listener.accept(themes());
        return once(() -> themeObservers.remove(listener));
    }

    private void refresh() {
        EditorColorThemeId effectiveId = effectiveThemeId();
        EditorAppearanceSnapshot replacement = effectiveId == null
                ? null
                : new EditorAppearanceSnapshot(resolve(effectiveId), editorFontFamily, editorFontSize);
        if (!Objects.equals(current, replacement)) {
            current = replacement;
            if (replacement != null) {
                List.copyOf(appearanceObservers).forEach(observer -> observer.accept(replacement));
            }
        }
    }

    private @Nullable EditorColorThemeId effectiveThemeId() {
        if (contributions.containsKey(requestedTheme)) {
            return requestedTheme;
        }
        return contributions.containsKey(DEFAULT_THEME) ? DEFAULT_THEME : null;
    }

    private EditorResolvedColorTheme resolve(EditorColorThemeId id) {
        EditorColorTheme contribution = contributions.get(id);
        if (contribution == null) {
            throw new IllegalArgumentException("color theme is not registered: " + id.value());
        }
        Map<EditorColorTokenId, EditorColor> colors = new LinkedHashMap<>();
        Map<EditorSyntaxTokenId, EditorSyntaxStyle> syntaxStyles = new LinkedHashMap<>();
        contribution.parent().ifPresent(parent -> {
            EditorResolvedColorTheme inherited = resolve(parent);
            colors.putAll(inherited.colors());
            syntaxStyles.putAll(inherited.syntaxStyles());
        });
        colors.putAll(contribution.colors());
        syntaxStyles.putAll(contribution.syntaxStyles());
        return new EditorResolvedColorTheme(
                contribution.id(), contribution.label(), contribution.kind(), colors, syntaxStyles);
    }

    private @Nullable EditorResolvedColorTheme resolveIfPossible(EditorColorThemeId id) {
        try {
            return requireComplete(resolve(id));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static EditorResolvedColorTheme requireComplete(EditorResolvedColorTheme theme) {
        List<String> missing = EditorColorTokens.required().stream()
                .filter(token -> !theme.colors().containsKey(token))
                .map(EditorColorTokenId::value)
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("resolved color theme is missing required tokens: " + missing);
        }
        return theme;
    }

    private void notifyThemeObservers() {
        List<EditorResolvedColorTheme> snapshot = themes();
        List.copyOf(themeObservers).forEach(observer -> observer.accept(snapshot));
    }

    private void rememberPreferredTheme(EditorResolvedColorTheme selected) {
        if (isDark(selected.kind())) {
            preferredDarkTheme = selected.id();
            preferences.savePreferredDarkTheme(selected.id());
        } else {
            preferredLightTheme = selected.id();
            preferences.savePreferredLightTheme(selected.id());
        }
    }

    private EditorColorThemeId preferredTheme(boolean dark) {
        EditorColorThemeId preferred = dark ? preferredDarkTheme : preferredLightTheme;
        EditorResolvedColorTheme preferredContribution = resolveIfPossible(preferred);
        if (preferredContribution != null && isDark(preferredContribution.kind()) == dark) {
            return preferred;
        }
        EditorColorThemeId fallback = dark ? DEFAULT_THEME : DEFAULT_LIGHT_THEME;
        EditorResolvedColorTheme fallbackContribution = resolveIfPossible(fallback);
        if (fallbackContribution != null && isDark(fallbackContribution.kind()) == dark) {
            return fallback;
        }
        return themes().stream()
                .filter(theme -> isDark(theme.kind()) == dark)
                .map(EditorResolvedColorTheme::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "no " + (dark ? "dark" : "light") + " color theme has been registered"));
    }

    private static boolean isDark(EditorColorThemeKind kind) {
        return kind == EditorColorThemeKind.DARK || kind == EditorColorThemeKind.HIGH_CONTRAST_DARK;
    }

    private static EditorRegistration once(Runnable removal) {
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                removal.run();
            }
        };
    }
}
