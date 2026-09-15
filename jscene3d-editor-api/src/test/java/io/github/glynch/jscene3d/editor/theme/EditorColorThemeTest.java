/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies theme validation and the required semantic color contract. */
final class EditorColorThemeTest {
    @Test
    void exposesEveryRequiredColorToken() {
        assertThat(EditorColorTokens.required())
                .contains(EditorColorTokens.CANVAS, EditorColorTokens.ACCENT, EditorColorTokens.ERROR)
                .hasSize(32);
    }

    @Test
    void copiesThemeMapsAndRejectsInvalidThemes() {
        EditorColorThemeId id = new EditorColorThemeId("io.github.glynch.test.theme");
        Map<EditorColorTokenId, EditorColor> colors = Map.of(EditorColorTokens.CANVAS, EditorColor.rgb(1, 2, 3));
        EditorColorTheme theme =
                new EditorColorTheme(id, "Test", EditorColorThemeKind.DARK, Optional.empty(), colors, Map.of());
        Optional<EditorColorThemeId> noParent = Optional.empty();
        Optional<EditorColorThemeId> selfParent = Optional.of(id);
        Map<EditorColorTokenId, EditorColor> noColors = Map.of();
        Map<EditorSyntaxTokenId, EditorSyntaxStyle> noSyntaxStyles = Map.of();
        String blankLabel = " ";
        String label = "Test";

        assertThat(theme.colors()).containsExactlyEntriesOf(colors);
        assertThatThrownBy(() -> new EditorColorTheme(
                        id, blankLabel, EditorColorThemeKind.DARK, noParent, noColors, noSyntaxStyles))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EditorColorTheme(
                        id, label, EditorColorThemeKind.DARK, selfParent, noColors, noSyntaxStyles))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        new EditorColorTheme(id, label, EditorColorThemeKind.DARK, noParent, null, noSyntaxStyles))
                .isInstanceOf(NullPointerException.class);
    }
}
