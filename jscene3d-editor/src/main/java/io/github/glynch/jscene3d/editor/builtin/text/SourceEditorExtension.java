/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.file.EditorFileKind;
import io.github.glynch.jscene3d.editor.file.EditorFileType;
import io.github.glynch.jscene3d.editor.file.EditorFileTypeId;
import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Built-in extension associating Java, JSON, XML, and Maven files with the source editor. */
public final class SourceEditorExtension implements EditorExtension {
    private static final String TYPE_PREFIX = "io.github.glynch.jscene3d.editor.file-type.";

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.source-editor";
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(),
                "Source Editor",
                "Edits UTF-8 Java, JSON, XML, Maven, and plain-text workspace files.",
                "JScene3D",
                Optional.empty(),
                true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions()
                .add(editor.fileTypes()
                        .register(textType(
                                "maven",
                                "Maven project",
                                EditorLanguages.XML,
                                EditorIcons.MAVEN,
                                Set.of("pom.xml"),
                                Set.of(),
                                100)));
        editor.subscriptions()
                .add(editor.fileTypes()
                        .register(textType(
                                "java",
                                "Java source",
                                EditorLanguages.JAVA,
                                EditorIcons.JAVA,
                                Set.of(),
                                Set.of("java"),
                                0)));
        editor.subscriptions()
                .add(editor.fileTypes()
                        .register(textType(
                                "json",
                                "JSON document",
                                EditorLanguages.JSON,
                                EditorIcons.JSON,
                                Set.of(),
                                Set.of("json", "jsonc"),
                                0)));
        editor.subscriptions()
                .add(editor.fileTypes()
                        .register(textType(
                                "xml",
                                "XML document",
                                EditorLanguages.XML,
                                EditorIcons.XML,
                                Set.of(),
                                Set.of("xml", "xsd", "dtd", "xsl", "xslt"),
                                0)));
    }

    private static EditorFileType textType(
            String id,
            String label,
            EditorLanguageId language,
            EditorIconId icon,
            Set<String> fileNames,
            Set<String> extensions,
            int priority) {
        return new EditorFileType(
                new EditorFileTypeId(TYPE_PREFIX + id),
                label,
                EditorFileKind.TEXT,
                Optional.of(language),
                new EditorIcon(icon, label),
                fileNames,
                extensions,
                priority);
    }
}
