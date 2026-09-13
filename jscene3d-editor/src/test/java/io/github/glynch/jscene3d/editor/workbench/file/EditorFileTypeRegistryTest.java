/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.editor.file.EditorFileKind;
import io.github.glynch.jscene3d.editor.file.EditorFileType;
import io.github.glynch.jscene3d.editor.file.EditorFileTypeId;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class EditorFileTypeRegistryTest {
    private static final EditorIcon ICON = new EditorIcon(EditorIcons.SOURCE_ASSET, "File");

    @Test
    void exactFilenameWinsOverExtensionAndRegistrationsAreRemovable() {
        EditorFileTypeRegistry registry = new EditorFileTypeRegistry();
        registry.register(type("xml", Set.of(), Set.of("xml"), 100));
        var maven = registry.register(type("maven", Set.of("pom.xml"), Set.of(), 0));

        assertThat(registry.resolve(URI.create("file:///workspace/pom.xml")))
                .get()
                .extracting(candidate -> candidate.id().value())
                .isEqualTo("io.github.glynch.test.maven");

        maven.close();

        assertThat(registry.resolve(URI.create("file:///workspace/pom.xml")))
                .get()
                .extracting(candidate -> candidate.id().value())
                .isEqualTo("io.github.glynch.test.xml");
    }

    @Test
    void rejectsDuplicateIdentitiesAndReturnsEmptyForUnknownFiles() {
        EditorFileTypeRegistry registry = new EditorFileTypeRegistry();
        EditorFileType javaType = type("java", Set.of(), Set.of("java"), 0);
        registry.register(javaType);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.register(javaType))
                .withMessageContaining("already registered");
        assertThat(registry.resolve(URI.create("file:///workspace/README.md"))).isEmpty();
    }

    private static EditorFileType type(String name, Set<String> fileNames, Set<String> extensions, int priority) {
        return new EditorFileType(
                new EditorFileTypeId("io.github.glynch.test." + name),
                name,
                EditorFileKind.TEXT,
                Optional.of(EditorLanguages.XML),
                ICON,
                fileNames,
                extensions,
                priority);
    }
}
