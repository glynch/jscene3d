/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.icon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaFxIconRegistryTest {
    @Test
    void resolvesBuiltInGlyphsAndSemanticTones() {
        JavaFxIconRegistry icons = JavaFxIconRegistry.builtIn();

        assertThat(icons.resolve(EditorIcons.PROJECT).path()).isNotBlank();
        assertThat(icons.resolve(EditorIcons.PROJECT).tone()).isEqualTo(JavaFxIconGlyph.Tone.PRIMARY);
        assertThat(icons.resolve(EditorIcons.READ_ONLY).tone()).isEqualTo(JavaFxIconGlyph.Tone.READ_ONLY);
        assertThat(icons.resolve(EditorIcons.DISABLED).tone()).isEqualTo(JavaFxIconGlyph.Tone.DISABLED);
        assertThat(icons.resolve(EditorIcons.MODIFIED).tone()).isEqualTo(JavaFxIconGlyph.Tone.WARNING);
        assertThat(icons.resolve(EditorIcons.MODIFIED).path()).isEqualTo("M8 4A4 4 0 1 0 8 12A4 4 0 1 0 8 4Z");
        assertThat(icons.resolve(EditorIcons.ERROR).tone()).isEqualTo(JavaFxIconGlyph.Tone.ERROR);
        assertThat(icons.resolve(EditorIcons.WARNING).tone()).isEqualTo(JavaFxIconGlyph.Tone.WARNING);
        assertThat(icons.resolve(EditorIcons.INFORMATION).tone()).isEqualTo(JavaFxIconGlyph.Tone.INFORMATION);
        assertThat(List.of(EditorIcons.PRIMARY_SIDEBAR, EditorIcons.PANEL, EditorIcons.SECONDARY_SIDEBAR))
                .map(icon -> icons.resolve(icon).path())
                .doesNotHaveDuplicates();
    }

    @Test
    void usesTheThemeFallbackForUnknownExtensionIcons() {
        JavaFxIconRegistry icons = JavaFxIconRegistry.load(getClass(), "test-icons.properties");

        assertThat(icons.resolve(new EditorIconId("example.extension.icon.known"))
                        .path())
                .isEqualTo("known-path");
        assertThat(icons.resolve(new EditorIconId("example.extension.icon.unknown"))
                        .path())
                .isEqualTo("fallback-path");
    }

    @Test
    void rejectsInvalidThemeMetadataWhenTheThemeIsLoaded() {
        assertThatIllegalStateException()
                .isThrownBy(() -> JavaFxIconRegistry.load(getClass(), "invalid-icons.properties"))
                .withMessageContaining("invalid icon tone")
                .withMessageContaining("example.extension.icon.invalid.tone");
    }
}
