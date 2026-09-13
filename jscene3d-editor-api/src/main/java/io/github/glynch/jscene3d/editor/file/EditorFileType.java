/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.file;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Declarative association between workspace filenames and a built-in editor family.
 *
 * @param id stable contribution identity
 * @param label non-blank human-readable type name
 * @param kind editor presentation family
 * @param language source-editor language, required for text and absent for images
 * @param icon semantic Explorer and tab icon
 * @param fileNames case-insensitive exact filenames
 * @param extensions lowercase extensions without a leading dot
 * @param priority higher values win between associations with equal specificity
 */
public record EditorFileType(
        EditorFileTypeId id,
        String label,
        EditorFileKind kind,
        Optional<EditorLanguageId> language,
        EditorIcon icon,
        Set<String> fileNames,
        Set<String> extensions,
        int priority) {
    /** Copies and validates one complete file-type contribution. */
    public EditorFileType {
        Objects.requireNonNull(id, "id");
        if (Objects.requireNonNull(label, "label").isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(icon, "icon");
        fileNames = normalized(fileNames, "fileNames", false);
        extensions = normalized(extensions, "extensions", true);
        if (fileNames.isEmpty() && extensions.isEmpty()) {
            throw new IllegalArgumentException("a file type must match at least one filename or extension");
        }
        if ((kind == EditorFileKind.TEXT) != language.isPresent()) {
            throw new IllegalArgumentException("text file types require a language and image file types must omit it");
        }
    }

    private static Set<String> normalized(Set<String> values, String name, boolean rejectLeadingDot) {
        Objects.requireNonNull(values, name);
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException(name + " must not contain null");
        }
        if (values.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(name + " must not contain blank values");
        }
        if (rejectLeadingDot && values.stream().anyMatch(value -> value.startsWith("."))) {
            throw new IllegalArgumentException("extensions must not start with a dot");
        }
        return values.stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
