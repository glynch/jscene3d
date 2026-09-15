/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifies normalization and the text-versus-image invariants of file-type contributions. */
final class EditorFileTypeTest {
    @Test
    void normalizesTextFileNamesAndExtensions() {
        EditorFileType type = new EditorFileType(
                new EditorFileTypeId("io.github.glynch.test.text"),
                "Text",
                EditorFileKind.TEXT,
                Optional.of(new EditorLanguageId("text")),
                new EditorIcon(EditorIcons.TEXT_FILE, "Text file"),
                Set.of("README.MD"),
                Set.of("JSON", "Xml"),
                10);

        assertThat(type.fileNames()).containsExactly("readme.md");
        assertThat(type.extensions()).containsExactlyInAnyOrder("json", "xml");
    }

    @Test
    void acceptsImageTypesWithoutALanguage() {
        EditorFileType type = new EditorFileType(
                new EditorFileTypeId("io.github.glynch.test.image"),
                "Image",
                EditorFileKind.IMAGE,
                Optional.empty(),
                new EditorIcon(EditorIcons.IMAGE, "Image file"),
                Set.of(),
                Set.of("png"),
                0);

        assertThat(type.language()).isEmpty();
    }

    @Test
    void rejectsInvalidContributionShapes() {
        Optional<EditorLanguageId> noLanguage = Optional.empty();
        Optional<EditorLanguageId> imageLanguage = Optional.of(new EditorLanguageId("image"));
        Optional<EditorLanguageId> textLanguage = Optional.of(new EditorLanguageId("text"));
        Set<String> noNames = Set.of();
        Set<String> textExtension = Set.of("txt");
        Set<String> imageExtension = Set.of("png");
        Set<String> noExtensions = Set.of();
        Set<String> blankName = Set.of(" ");
        Set<String> dottedExtension = Set.of(".txt");

        assertThatThrownBy(() -> type(EditorFileKind.TEXT, noLanguage, noNames, textExtension))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> type(EditorFileKind.IMAGE, imageLanguage, noNames, imageExtension))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> type(EditorFileKind.TEXT, textLanguage, noNames, noExtensions))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> type(EditorFileKind.TEXT, textLanguage, blankName, textExtension))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> type(EditorFileKind.TEXT, textLanguage, noNames, dottedExtension))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static EditorFileType type(
            EditorFileKind kind, Optional<EditorLanguageId> language, Set<String> fileNames, Set<String> extensions) {
        return new EditorFileType(
                new EditorFileTypeId("io.github.glynch.test.type"),
                "Type",
                kind,
                language,
                new EditorIcon(EditorIcons.TEXT_FILE, "Type file"),
                fileNames,
                extensions,
                0);
    }
}
