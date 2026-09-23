/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKindId;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Creates the common selection and view structure surrounding Inspector sections. */
final class EditorInspectorViewFactory {
    private EditorInspectorViewFactory() {
        throw new AssertionError("EditorInspectorViewFactory cannot be instantiated");
    }

    static EditorDetails.Section section(String title, List<EditorDetails.Property> properties) {
        return new EditorDetails.Section(title, Optional.empty(), true, properties);
    }

    static EditorDetails view(
            String title,
            String kind,
            Path source,
            Path projectRoot,
            String identity,
            Editability editability,
            List<EditorDetails.Section> sections) {
        String tooltip = editability == Editability.GENERATED_READ_ONLY ? "Generated content · read-only" : "Read-only";
        return new EditorDetails(
                title,
                kind,
                displaySource(source, projectRoot),
                identity,
                editability == Editability.EDITABLE
                        ? List.of()
                        : List.of(new EditorIcon(EditorIcons.READ_ONLY, tooltip)),
                sections);
    }

    static Editability editability(boolean generated, boolean editable) {
        if (editable) {
            return Editability.EDITABLE;
        }
        return generated ? Editability.GENERATED_READ_ONLY : Editability.READ_ONLY;
    }

    static EditorSelection selection(EditorSelectionKindId kind, Path source, String identity, EditorDetails details) {
        String key = kind.value() + ':' + source.toAbsolutePath().normalize() + ':' + identity;
        return new EditorSelection(kind, key, details);
    }

    private static String displaySource(Path source, Path projectRoot) {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        return normalizedSource.startsWith(normalizedRoot)
                ? normalizedRoot.relativize(normalizedSource).toString()
                : normalizedSource.toString();
    }

    enum Editability {
        EDITABLE,
        READ_ONLY,
        GENERATED_READ_ONLY
    }
}
