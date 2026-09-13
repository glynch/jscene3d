/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import static io.github.glynch.jscene3d.editor.theme.EditorColor.parseHex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.builtin.appearance.BuiltinColorThemesExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.theme.EditorColorTheme;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeKind;
import io.github.glynch.jscene3d.editor.theme.EditorColorTokens;
import io.github.glynch.jscene3d.editor.workbench.configuration.EditorConfigurationContext;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class EditorColorThemeRegistryTest {
    private static final EditorColorThemeId EXTERNAL_THEME =
            new EditorColorThemeId("io.github.glynch.example.theme.ocean");
    private static final EditorColorThemeId EXTERNAL_LIGHT_THEME =
            new EditorColorThemeId("io.github.glynch.example.theme.paper");

    @Test
    void externalExtensionContributesAnInheritedThemeWithoutToolkitTypes() {
        EditorColorThemeRegistry themes = registry();
        try (EditorExtensionHost host = new EditorExtensionHost(
                new EditorProjectContext(), new EditorSelectionContext(), new EditorConfigurationContext(), themes)) {
            host.activate(new BuiltinColorThemesExtension());
            host.activate(new OceanThemeExtension());

            themes.select(EXTERNAL_THEME);

            assertThat(themes.current().colorTheme()).satisfies(theme -> {
                assertThat(theme.label()).isEqualTo("Ocean");
                assertThat(theme.color(EditorColorTokens.ACCENT)).isEqualTo(parseHex("#00aacc"));
                assertThat(theme.color(EditorColorTokens.CANVAS)).isEqualTo(parseHex("#0d1118"));
            });
        }
    }

    @Test
    void storedExternalSelectionBecomesActiveWhenItsExtensionRegisters() {
        InMemoryEditorAppearancePreferences preferences = new InMemoryEditorAppearancePreferences();
        preferences.saveColorTheme(EXTERNAL_THEME);
        EditorColorThemeRegistry themes = new EditorColorThemeRegistry(preferences);
        BuiltinColorThemesExtension.registerThemes(themes, ignored -> {});
        assertThat(themes.current().colorTheme().id()).isEqualTo(BuiltinColorThemesExtension.DARK);

        themes.register(oceanTheme());

        assertThat(themes.current().colorTheme().id()).isEqualTo(EXTERNAL_THEME);
    }

    @Test
    void selectionAndTypographyNotifyOpenAdaptersAndPersist() {
        RecordingPreferences preferences = new RecordingPreferences();
        EditorColorThemeRegistry themes = new EditorColorThemeRegistry(preferences);
        BuiltinColorThemesExtension.registerThemes(themes, ignored -> {});
        List<EditorAppearanceSnapshot> observed = new ArrayList<>();
        themes.observeAppearance(observed::add);

        themes.select(BuiltinColorThemesExtension.LIGHT);
        themes.setEditorFont("JetBrains Mono", 15);

        assertThat(observed).hasSize(3);
        assertThat(observed.getLast()).satisfies(appearance -> {
            assertThat(appearance.colorTheme().id()).isEqualTo(BuiltinColorThemesExtension.LIGHT);
            assertThat(appearance.editorFontFamily()).isEqualTo("JetBrains Mono");
            assertThat(appearance.editorFontSize()).isEqualTo(15);
        });
        assertThat(preferences.savedTheme).isEqualTo(BuiltinColorThemesExtension.LIGHT);
        assertThat(preferences.savedLightTheme).isEqualTo(BuiltinColorThemesExtension.LIGHT);
        assertThat(preferences.savedFamily).isEqualTo("JetBrains Mono");
        assertThat(preferences.savedSize).isEqualTo(15);
    }

    @Test
    void builtInLightThemeUsesSubduedLayeredSurfaces() {
        EditorColorThemeRegistry themes = registry();
        BuiltinColorThemesExtension.registerThemes(themes, ignored -> {});

        EditorResolvedColorTheme light = themes.themes().stream()
                .filter(theme -> theme.id().equals(BuiltinColorThemesExtension.LIGHT))
                .findFirst()
                .orElseThrow();

        assertThat(light.colors())
                .containsEntry(EditorColorTokens.CANVAS, parseHex("#d8dde6"))
                .containsEntry(EditorColorTokens.CHROME, parseHex("#e7eaf0"))
                .containsEntry(EditorColorTokens.PANEL, parseHex("#d2d8e2"))
                .containsEntry(EditorColorTokens.PANEL_RAISED, parseHex("#eceff3"))
                .containsEntry(EditorColorTokens.FIELD, parseHex("#f1f3f6"));
    }

    @Test
    void togglesBetweenRememberedDarkAndLightThemeFamilies() {
        EditorColorThemeRegistry themes = registry();
        BuiltinColorThemesExtension.registerThemes(themes, ignored -> {});
        themes.register(oceanTheme());
        themes.register(paperTheme());

        themes.select(EXTERNAL_LIGHT_THEME);
        themes.select(EXTERNAL_THEME);
        themes.toggleColorScheme();

        assertThat(themes.current().colorTheme().id()).isEqualTo(EXTERNAL_LIGHT_THEME);

        themes.toggleColorScheme();

        assertThat(themes.current().colorTheme().id()).isEqualTo(EXTERNAL_THEME);
    }

    @Test
    void rejectsMissingParentsAndIncompleteRootThemes() {
        EditorColorThemeRegistry themes = registry();
        EditorColorThemeId missing = new EditorColorThemeId("io.github.glynch.example.theme.missing");
        EditorColorTheme missingParent = new EditorColorTheme(
                EXTERNAL_THEME, "Ocean", EditorColorThemeKind.DARK, Optional.of(missing), Map.of(), Map.of());

        assertThatThrownBy(() -> themes.register(missingParent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent is not registered");

        EditorColorTheme incompleteRoot = new EditorColorTheme(
                EXTERNAL_THEME,
                "Ocean",
                EditorColorThemeKind.DARK,
                Optional.empty(),
                Map.of(EditorColorTokens.ACCENT, parseHex("#00aacc")),
                Map.of());
        assertThatThrownBy(() -> themes.register(incompleteRoot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required tokens");
    }

    private static EditorColorThemeRegistry registry() {
        return new EditorColorThemeRegistry(new InMemoryEditorAppearancePreferences());
    }

    private static EditorColorTheme oceanTheme() {
        return new EditorColorTheme(
                EXTERNAL_THEME,
                "Ocean",
                EditorColorThemeKind.DARK,
                Optional.of(BuiltinColorThemesExtension.DARK),
                Map.of(EditorColorTokens.ACCENT, parseHex("#00aacc")),
                Map.of());
    }

    private static EditorColorTheme paperTheme() {
        return new EditorColorTheme(
                EXTERNAL_LIGHT_THEME,
                "Paper",
                EditorColorThemeKind.HIGH_CONTRAST_LIGHT,
                Optional.of(BuiltinColorThemesExtension.LIGHT),
                Map.of(EditorColorTokens.ACCENT, parseHex("#663399")),
                Map.of());
    }

    private static final class OceanThemeExtension implements EditorExtension {
        @Override
        public String id() {
            return "io.github.glynch.example.ocean-theme";
        }

        @Override
        public EditorExtensionDescriptor descriptor() {
            return new EditorExtensionDescriptor(
                    id(),
                    "Ocean Theme",
                    "Contributes an inherited editor color theme.",
                    "Example",
                    Optional.empty(),
                    false);
        }

        @Override
        public void activate(EditorExtensionContext editor) {
            editor.subscriptions().add(editor.colorThemes().register(oceanTheme()));
        }
    }

    private static final class RecordingPreferences implements EditorAppearancePreferences {
        private EditorColorThemeId savedTheme = EditorColorThemeRegistry.DEFAULT_THEME;
        private EditorColorThemeId savedDarkTheme = EditorColorThemeRegistry.DEFAULT_THEME;
        private EditorColorThemeId savedLightTheme = EditorColorThemeRegistry.DEFAULT_LIGHT_THEME;
        private String savedFamily = "Monospaced";
        private int savedSize = 13;

        @Override
        public EditorColorThemeId colorTheme(EditorColorThemeId fallback) {
            return fallback;
        }

        @Override
        public EditorColorThemeId preferredDarkTheme(EditorColorThemeId fallback) {
            return savedDarkTheme;
        }

        @Override
        public EditorColorThemeId preferredLightTheme(EditorColorThemeId fallback) {
            return savedLightTheme;
        }

        @Override
        public String editorFontFamily(String fallback) {
            return fallback;
        }

        @Override
        public int editorFontSize(int fallback) {
            return fallback;
        }

        @Override
        public void saveColorTheme(EditorColorThemeId theme) {
            savedTheme = theme;
        }

        @Override
        public void savePreferredDarkTheme(EditorColorThemeId theme) {
            savedDarkTheme = theme;
        }

        @Override
        public void savePreferredLightTheme(EditorColorThemeId theme) {
            savedLightTheme = theme;
        }

        @Override
        public void saveEditorFont(String family, int size) {
            savedFamily = family;
            savedSize = size;
        }
    }
}
